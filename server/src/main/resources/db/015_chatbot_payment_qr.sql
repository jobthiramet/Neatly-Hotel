-- Run in Supabase SQL Editor after 014_chatbot_script_seed.sql.
-- Adds QR Code to an existing chatbot payment-methods topic. Safe to run more than once.

update chatbot_script as script
set
  topics = (
    select jsonb_agg(
      case
        when topic->>'id' = 'payment-methods'
          and not exists (
            select 1
            from jsonb_array_elements(coalesce(topic->'options', '[]'::jsonb)) as option
            where option->>'label' = 'QR Code'
          )
        then jsonb_set(
          topic,
          '{options}',
          coalesce(topic->'options', '[]'::jsonb) || jsonb_build_array(
            jsonb_build_object(
              'label', 'QR Code',
              'detail', 'You can pay by scanning a QR code, including PromptPay.'
            )
          )
        )
        else topic
      end
      order by ordinality
    )::text
    from jsonb_array_elements(script.topics::jsonb) with ordinality as item(topic, ordinality)
  ),
  updated_at = current_timestamp
where script.topics::jsonb @> '[{"id":"payment-methods"}]'::jsonb;
