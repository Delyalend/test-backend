create table author (
    id serial primary key,
    fio varchar(300) not null,
    created_at timestamp null
);