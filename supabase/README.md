# Supabase Backend

This folder tracks the SQL contract for the Supabase prototype path.

Run `schema.sql` in the Supabase SQL Editor after creating the project and enabling Anonymous Sign-ins. The file creates:

- authenticated profiles with FCM tokens;
- public pair codes;
- pair requests;
- accepted pairs;
- per-member sent and received unread counters;
- beat event history for future timelines;
- RLS policies and RPC functions used by the Android client.

The current Android app still uses the Firebase repository. Supabase is the next backend direction because Firebase Cloud Functions / Blaze is blocked by the billing-card requirement.
