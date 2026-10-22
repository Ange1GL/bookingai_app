-- Users keep their national number (10 digits); the "52" country code is added only when sending by WhatsApp.
update user_app set phone = substr(phone, 3) where phone ~ '^52[0-9]{10}$';

alter table user_app alter column phone type varchar(10);
