create table products
(
    id          bigserial primary key,
    code        varchar not null unique,
    name        varchar not null,
    description varchar,
    image       varchar,
    price       numeric not null
);

insert into products(code, name, description, image, price) values
    ('P101','Product P101','Product P101 description', null, 34.0),
    ('P102','Product P102','Product P102 description', null, 25.0),
    ('P103','Product P103','Product P103 description', null, 15.0)
;