CREATE TABLE USERS(
username varchar(20) PRIMARY KEY,
password_hash varbinary(100),
password_salt varbinary(100),
balance int);
CREATE TABLE Reservations(
rid int primary key,
username varchar(20) references users,
price int NOT NULL,
paid bit NOT NULL,
rday int,
fid1 int,
fid2 int,
);
CREATE TABLE BOOKING(
fid int references flights,
capacity int references flights,
capacity_taken int,
bday int,
username varchar(20)
);