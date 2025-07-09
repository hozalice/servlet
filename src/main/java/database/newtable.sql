    CREATE TABLE date_pourcentage(
        daty DATE,
        pourcentage DOUBLE,
        signe VARCHAR(50)
    );
INSERT INTO date_pourcentage (daty, pourcentage,signe) VALUES
                                                     ('2025/01/01', 50,'-'),
                                                     ('2025/02/01', 25,'+'),
                                                     ('2025/03/01', 10,'-');
