create table proxy_mail (
  proxy_address             varchar(255) not null,
  user_id                   varchar(40),
  creation_time             timestamp not null,
  lang                      varchar(255) not null,
  expiration_time           timestamp not null,
  blocked                   boolean,
  active                    boolean,
  expiration_notified       boolean,
  constraint pk_proxy_mail primary key (proxy_address))
;

create table users (
  id                        varchar(40) not null,
  email_address             varchar(255) not null,
  creation_time             timestamp not null,
  constraint pk_users primary key (id))
;

create sequence proxy_mail_seq;

alter table proxy_mail add constraint fk_proxy_mail_user_1 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_proxy_mail_user_1 on proxy_mail (user_id);


