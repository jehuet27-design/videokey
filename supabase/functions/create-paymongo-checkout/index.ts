// Supabase Edge Function: create-paymongo-checkout
// Creates a ₱110.00 PayMongo Checkout Session for GCash payment bound to Android Device ID

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Content-Type": "application/json",
};

serve(async (req: Request) => {
  // Handle CORS preflight request
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { device_id } = await req.json();

    if (!device_id || typeof device_id !== "string" || device_id.trim().length < 4) {
      return new Response(
        JSON.stringify({ error: "Valid device_id is required" }),
        { status: 400, headers: corsHeaders }
      );
    }

    const cleanDeviceId = device_id.trim();

    // 1. Environment keys
    const paymongoSecretKey = Deno.env.get("PAYMONGO_SECRET_KEY");
    if (!paymongoSecretKey) {
      throw new Error("PAYMONGO_SECRET_KEY is not configured in Supabase Secrets");
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // 2. Prepare PayMongo Checkout Session payload
    // ₱110.00 = 11000 centavos (covers ₱99 VIP + ₱4 transaction fee)
    const authHeader = "Basic " + btoa(`${paymongoSecretKey}:`);

    const checkoutPayload = {
      data: {
        attributes: {
          send_email_receipt: false,
          show_description: true,
          show_line_items: true,
          description: `VideoKey VIP Lifetime License bound to ${cleanDeviceId}`,
          line_items: [
            {
              currency: "PHP",
              amount: 11000, // ₱110.00
              name: "VideoKey VIP Lifetime License",
              quantity: 1,
              description: `Device ID: ${cleanDeviceId} • Permanent VIP Access to 57k Songs & Studio Audio`,
            },
          ],
          payment_method_types: ["gcash", "paymaya", "card", "grab_pay"],
          success_url: `videokey://payment-success?device_id=${encodeURIComponent(cleanDeviceId)}`,
          cancel_url: `videokey://payment-cancel`,
          metadata: {
            device_id: cleanDeviceId,
            app: "VideoKey",
            product: "vip_lifetime_license",
          },
        },
      },
    };

    // 3. Call PayMongo API
    const paymongoRes = await fetch("https://api.paymongo.com/v1/checkout_sessions", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: authHeader,
      },
      body: JSON.stringify(checkoutPayload),
    });

    const responseJson = await paymongoRes.json();

    if (!paymongoRes.ok || !responseJson.data) {
      console.error("PayMongo error response:", JSON.stringify(responseJson));
      return new Response(
        JSON.stringify({
          error: "Failed to create PayMongo checkout session",
          details: responseJson.errors ?? responseJson,
        }),
        { status: 502, headers: corsHeaders }
      );
    }

    const checkoutSession = responseJson.data;
    const checkoutUrl = checkoutSession.attributes.checkout_url;
    const checkoutSessionId = checkoutSession.id;

    // 4. Save initial pending record in Supabase orders table
    const { error: dbError } = await supabase.from("orders").insert({
      device_id: cleanDeviceId,
      checkout_session_id: checkoutSessionId,
      amount: 110.00,
      currency: "PHP",
      status: "pending",
      payment_method: "gcash",
      metadata: {
        checkout_url: checkoutUrl,
        created_via: "create-paymongo-checkout",
      },
    });

    if (dbError) {
      console.warn("Notice: Failed to insert order record, proceeding with checkout URL:", dbError.message);
    }

    // 5. Return checkout URL to Android app
    return new Response(
      JSON.stringify({
        success: true,
        device_id: cleanDeviceId,
        checkout_session_id: checkoutSessionId,
        checkout_url: checkoutUrl,
      }),
      { status: 200, headers: corsHeaders }
    );
  } catch (err: any) {
    console.error("Internal error creating checkout:", err);
    return new Response(
      JSON.stringify({ error: err.message ?? "Internal server error" }),
      { status: 500, headers: corsHeaders }
    );
  }
});
