alter table budget
add column author_id int null references author(id)
