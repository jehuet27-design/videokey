// Supabase Edge Function: paymongo-webhook
// Handles PayMongo webhook events (checkout_session.paid) and automatically activates VIP license

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, paymongo-signature",
  "Content-Type": "application/json",
};

/**
 * Generates an 8-character unique VIP key: VIP-XXXX-YYYY
 */
function generateVipKey(deviceId: string): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let randomPart = "";
  const randomBytes = new Uint8Array(8);
  crypto.getRandomValues(randomBytes);
  for (let i = 0; i < 8; i++) {
    randomPart += chars[randomBytes[i] % chars.length];
  }
  const part1 = randomPart.substring(0, 4);
  const part2 = randomPart.substring(4, 8);
  return `VIP-${part1}-${part2}`;
}

/**
 * Validates PayMongo Webhook Signature (HMAC-SHA256)
 */
async function verifyPayMongoSignature(
  signatureHeader: string | null,
  rawBody: string,
  webhookSecret: string
): Promise<boolean> {
  if (!signatureHeader || !webhookSecret) return true; // Allows bypass in dev if secret not configured

  try {
    // Header format: t=1612345678,te=test_signature,li=live_signature
    const parts = signatureHeader.split(",");
    let timestamp = "";
    let signature = "";

    for (const part of parts) {
      const [k, v] = part.split("=");
      if (k === "t") timestamp = v;
      if (k === "li" || k === "te") signature = v;
    }

    if (!timestamp || !signature) return false;

    const payload = `${timestamp}.${rawBody}`;
    const keyData = new TextEncoder().encode(webhookSecret);
    const cryptoKey = await crypto.subtle.importKey(
      "raw",
      keyData,
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    );

    const sigBuf = await crypto.subtle.sign(
      "HMAC",
      cryptoKey,
      new TextEncoder().encode(payload)
    );

    const expectedHex = Array.from(new Uint8Array(sigBuf))
      .map((b) => b.toString(16).padStart(2, "0"))
      .join("");

    return expectedHex.toLowerCase() === signature.toLowerCase();
  } catch (err) {
    console.error("Signature verification error:", err);
    return false;
  }
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const rawBody = await req.text();
    const signatureHeader = req.headers.get("paymongo-signature");
    const webhookSecret = Deno.env.get("PAYMONGO_WEBHOOK_SECRET") ?? "";

    // 1. Signature Verification
    if (webhookSecret) {
      const isValid = await verifyPayMongoSignature(signatureHeader, rawBody, webhookSecret);
      if (!isValid) {
        console.error("Invalid PayMongo webhook signature");
        return new Response(JSON.stringify({ error: "Invalid signature" }), {
          status: 401,
          headers: corsHeaders,
        });
      }
    }

    const event = JSON.parse(rawBody);
    const eventType = event?.data?.attributes?.type;

    console.log(`Received PayMongo Event: ${eventType}`);

    // We listen to checkout_session.paid or payment.paid
    if (eventType === "checkout_session.paid") {
      const sessionData = event.data.attributes.data;
      const checkoutSessionId = sessionData?.id;
      const attributes = sessionData?.attributes ?? {};
      const metadata = attributes.metadata ?? {};
      const deviceId = metadata.device_id?.trim();

      const paymentIntentId = attributes.payment_intent?.id ?? attributes.payments?.[0]?.id ?? null;

      if (!deviceId) {
        console.warn("No device_id found in checkout session metadata:", JSON.stringify(metadata));
        return new Response(JSON.stringify({ message: "No device_id in metadata" }), {
          status: 200,
          headers: corsHeaders,
        });
      }

      // Initialize Supabase Client
      const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
      const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
      const supabase = createClient(supabaseUrl, supabaseServiceKey);

      // 2. Generate unique VIP License Key
      const generatedVipKey = generateVipKey(deviceId);

      // 3. Upsert VIP License bound to this device_id
      const { data: licenseData, error: licenseError } = await supabase
        .from("vip_licenses")
        .upsert(
          {
            device_id: deviceId,
            license_key: generatedVipKey,
            is_active: true,
            activated_at: new Date().toISOString(),
            updated_at: new Date().toISOString(),
            order_id: checkoutSessionId,
            notes: "Automated PayMongo GCash Webhook Activation",
          },
          { onConflict: "device_id" }
        )
        .select()
        .single();

      if (licenseError) {
        console.error("Failed to upsert VIP license:", licenseError);
        return new Response(JSON.stringify({ error: licenseError.message }), {
          status: 500,
          headers: corsHeaders,
        });
      }

      // 4. Update order status to 'paid'
      if (checkoutSessionId) {
        await supabase
          .from("orders")
          .update({
            status: "paid",
            payment_intent_id: paymentIntentId,
            updated_at: new Date().toISOString(),
            metadata: {
              ...(metadata ?? {}),
              paid_at: new Date().toISOString(),
              activated_license_key: generatedVipKey,
            },
          })
          .eq("checkout_session_id", checkoutSessionId);
      }

      console.log(`[SUCCESS] Activated VIP License for Device: ${deviceId} with Key: ${generatedVipKey}`);

      return new Response(
        JSON.stringify({
          success: true,
          event: eventType,
          device_id: deviceId,
          license_key: generatedVipKey,
        }),
        { status: 200, headers: corsHeaders }
      );
    }

    // Acknowledge other events gracefully
    return new Response(JSON.stringify({ received: true, event: eventType }), {
      status: 200,
      headers: corsHeaders,
    });
  } catch (err: any) {
    console.error("Webhook processing error:", err);
    return new Response(
      JSON.stringify({ error: err.message ?? "Internal server error" }),
      { status: 500, headers: corsHeaders }
    );
  }
});
