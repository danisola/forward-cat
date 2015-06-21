create table proxy_mail (
  proxy_address             varchar(255) not null,
  user_address              varchar(255) not null,
  creation_time             timestamp not null,
  lang                      varchar(255) not null,
  expiration_time           timestamp not null,
  blocked                   boolean,
  active                    boolean,
  expiration_notified       boolean,
  constraint pk_proxy_mail primary key (proxy_address))
;

create table "user" (
  id                        varchar(40) not null,
  user_address              varchar(255) not null,
  constraint pk_user primary key (id))
;

create sequence proxy_mail_seq;

