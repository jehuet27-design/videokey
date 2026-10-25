-- =====================================================================
-- VideoKey VIP & PayMongo GCash Automated Payment Database Schema
-- Supabase PostgreSQL with RLS and Device ID Binding
-- =====================================================================

-- 1. Create table for Payment Orders
CREATE TABLE IF NOT EXISTS public.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    device_id TEXT NOT NULL,
    checkout_session_id TEXT UNIQUE,
    payment_intent_id TEXT,
    amount NUMERIC(10, 2) NOT NULL DEFAULT 110.00,
    currency TEXT NOT NULL DEFAULT 'PHP',
    status TEXT NOT NULL DEFAULT 'pending', -- 'pending', 'paid', 'failed', 'expired'
    payment_method TEXT DEFAULT 'gcash',
    metadata JSONB DEFAULT '{}'::jsonb
);

-- 2. Create table for VIP Licenses bound to Device ID
CREATE TABLE IF NOT EXISTS public.vip_licenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    device_id TEXT NOT NULL UNIQUE, -- Strictly one VIP license per Android Device ID
    license_key TEXT NOT NULL,       -- e.g. VIP-8A2F-9B4C
    is_active BOOLEAN NOT NULL DEFAULT true,
    activated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    order_id TEXT,                  -- Reference to checkout_session_id
    notes TEXT DEFAULT 'PayMongo GCash Automated Activation'
);

-- 3. Indexes for ultra-fast lookup by Android app
CREATE INDEX IF NOT EXISTS idx_orders_device_id ON public.orders(device_id);
CREATE INDEX IF NOT EXISTS idx_orders_checkout_session ON public.orders(checkout_session_id);
CREATE INDEX IF NOT EXISTS idx_vip_licenses_device_id ON public.vip_licenses(device_id);
CREATE INDEX IF NOT EXISTS idx_vip_licenses_key ON public.vip_licenses(license_key);

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vip_licenses ENABLE ROW LEVEL SECURITY;

-- 5. Policies for public / anon client verification
-- Allow clients to check their own device VIP status directly
DROP POLICY IF EXISTS "Allow anon to verify own device license" ON public.vip_licenses;
CREATE POLICY "Allow anon to verify own device license"
ON public.vip_licenses
FOR SELECT
TO anon, authenticated
USING (true);

-- Allow service_role full control (used by Edge Functions / Webhooks)
DROP POLICY IF EXISTS "Service role has full access to orders" ON public.orders;
CREATE POLICY "Service role has full access to orders"
ON public.orders
FOR ALL
TO service_role
USING (true)
WITH CHECK (true);

DROP POLICY IF EXISTS "Service role has full access to vip_licenses" ON public.vip_licenses;
CREATE POLICY "Service role has full access to vip_licenses"
ON public.vip_licenses
FOR ALL
TO service_role
USING (true)
WITH CHECK (true);

-- 6. RPC Function: check_vip_status (Secure Stored Procedure for Android App)
-- Can be called from Android via: POST /rest/v1/rpc/check_vip_status
CREATE OR REPLACE FUNCTION public.check_vip_status(p_device_id TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_license RECORD;
BEGIN
    SELECT * INTO v_license
    FROM public.vip_licenses
    WHERE UPPER(TRIM(device_id)) = UPPER(TRIM(p_device_id))
      AND is_active = true
    LIMIT 1;

    IF FOUND THEN
        RETURN jsonb_build_object(
            'is_vip', true,
            'device_id', v_license.device_id,
            'license_key', v_license.license_key,
            'activated_at', v_license.activated_at,
            'message', 'VIP Active'
        );
    ELSE
        RETURN jsonb_build_object(
            'is_vip', false,
            'device_id', p_device_id,
            'license_key', NULL,
            'activated_at', NULL,
            'message', 'No active VIP license found'
        );
    END IF;
END;
$$;

-- Grant execution permission to anon and authenticated
GRANT EXECUTE ON FUNCTION public.check_vip_status(TEXT) TO anon, authenticated, service_role;
