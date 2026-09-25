-- Portable seed for the local H2 profile and for Supabase after 014_chatbot_script.sql.
-- Keep the copy aligned with client/src/data/chatbot.ts.

insert into chatbot_script (id, created_at, updated_at, greeting, auto_reply, topics)
select
  '00000000-0000-0000-0000-000000000002',
  current_timestamp,
  current_timestamp,
  'Welcome to Neatly Hotel! 🌟 I''m your virtual assistant. Choose a topic you''d like to know more about. I''m here to help! 😊',
  'Thanks for reaching out to us! If you need any more help, just give us a call at 020872345 we''re happy to assist you! 🧡',
  '[{"id":"room-types","label":"Room Types","enabled":true,"format":"room-type","title":"Neatly Hotel offers a variety of room types to suit your needs! 🏨✨ Here are the options.","actionLabel":"View Details","roomIds":["superior-garden-view","deluxe","superior","supreme"]},{"id":"booking","label":"Booking","enabled":true,"format":"room-type","title":"Let''s get your booking started. First, please choose the type of room you''d like 🏨✨.","actionLabel":"Book Now","roomIds":["superior-garden-view","deluxe","superior","supreme"]},{"id":"check-in-out","label":"Check-in & Check-out Time","enabled":true,"format":"message","text":"Great! 😊 Here are our check-in and check-out times:\n\nCheck-in time: From 2:00 PM onwards 🕒\nCheck-out time: By 12:00 PM 🕛"},{"id":"payment-methods","label":"Payment Methods","enabled":true,"format":"option-with-details","title":"Here are the payment methods we accept. Tap to see more details 💳💰.","options":[{"label":"Credit Card","detail":"We accept credit cards including Visa and MasterCard."},{"label":"Cash","detail":"You can pay at the hotel with cash or cheque. No payment is required until check-in."},{"label":"QR Code","detail":"You can pay by scanning a QR code, including PromptPay."}]},{"id":"promotion","label":"Promotion","enabled":true,"format":"room-type","title":"🎉 Our promotion this month: Get 10% off 💰 when you book your stay within this month. Don''t miss out!","actionLabel":"Book Now","roomIds":["superior-garden-view","deluxe","superior","supreme"]}]'
where not exists (select 1 from chatbot_script);
