CREATE TABLE Accounts (
  AccountId        STRING(32) NOT NULL,
  AccountName      STRING(200) NOT NULL,
  Currency         STRING(3) NOT NULL,
  AvailableBalance NUMERIC NOT NULL,
  AccountStatus    STRING(20) NOT NULL,
  VersionNumber    INT64 NOT NULL,

  CreatedAt TIMESTAMP NOT NULL
    OPTIONS (allow_commit_timestamp = true),

  LastModifiedAt TIMESTAMP NOT NULL
    OPTIONS (allow_commit_timestamp = true),

  CONSTRAINT CK_Accounts_Balance
    CHECK (AvailableBalance >= 0),

  CONSTRAINT CK_Accounts_Status
    CHECK (
      AccountStatus IN ('ACTIVE', 'SUSPENDED', 'CLOSED')
    )
) PRIMARY KEY (AccountId);


CREATE TABLE Accounts (
  AccountId        STRING(32) NOT NULL,
  AccountName      STRING(200) NOT NULL,
  Currency         STRING(3) NOT NULL,
  AvailableBalance NUMERIC NOT NULL,
  AccountStatus    STRING(20) NOT NULL,
  VersionNumber    INT64 NOT NULL,

  CreatedAt TIMESTAMP NOT NULL
    OPTIONS (allow_commit_timestamp = true),

  LastModifiedAt TIMESTAMP NOT NULL
    OPTIONS (allow_commit_timestamp = true),

  CONSTRAINT CK_Accounts_Balance
    CHECK (AvailableBalance >= 0),

  CONSTRAINT CK_Accounts_Status
    CHECK (
      AccountStatus IN ('ACTIVE', 'SUSPENDED', 'CLOSED')
    )
) PRIMARY KEY (AccountId);