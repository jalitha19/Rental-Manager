ALTER TABLE rent_payments
    DROP CONSTRAINT rent_payments_rental_id_fkey,
    ADD CONSTRAINT rent_payments_rental_id_fkey
        FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE CASCADE;