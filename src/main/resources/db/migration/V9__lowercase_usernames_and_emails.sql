-- Usernames and e-mails are now stored and looked up in lower case, so the existing unique
-- constraints apply regardless of case ('Admin' and 'admin' are one account).
--
-- This fails on a unique constraint if two existing accounts differ only by case: decide which
-- one keeps the name (rename or remove the other), then migrate again.

UPDATE users
SET username = LOWER(username),
    email    = LOWER(email)
WHERE username <> LOWER(username)
   OR email <> LOWER(email);
