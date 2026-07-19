CREATE TABLE Customers (
  CustomerId INT64 NOT NULL,
  Name       STRING(255),
  Email      STRING(255),
  Country    STRING(100)
) PRIMARY KEY (CustomerId);

CREATE TABLE Orders (
  CustomerId  INT64 NOT NULL,
  OrderId     INT64 NOT NULL,
  OrderDate   DATE,
  TotalAmount NUMERIC
) PRIMARY KEY (CustomerId, OrderId),
  INTERLEAVE IN PARENT Customers ON DELETE CASCADE;

CREATE TABLE OrderItems (
  CustomerId  INT64 NOT NULL,
  OrderId     INT64 NOT NULL,
  OrderItemId INT64 NOT NULL,
  ProductId   INT64,
  Quantity    INT64,
  Price       NUMERIC
) PRIMARY KEY (CustomerId, OrderId, OrderItemId),
  INTERLEAVE IN PARENT Orders ON DELETE CASCADE;

CREATE INDEX OrdersByDate
  ON Orders(OrderDate);

CREATE INDEX OrdersByDateCovered
  ON Orders(OrderDate)
  STORING (TotalAmount);