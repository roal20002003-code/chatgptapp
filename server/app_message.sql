-- جدول پیام‌های اپ
CREATE TABLE dbo.app_message (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100)  NULL,
    contact     NVARCHAR(150)  NULL,
    body        NVARCHAR(2000) NOT NULL,
    device      NVARCHAR(150)  NULL,
    app_version NVARCHAR(20)   NULL,
    ip          NVARCHAR(50)   NULL,
    created     DATETIME       NOT NULL DEFAULT GETDATE(),
    is_read     BIT            NOT NULL DEFAULT 0
);

-- دیدن پیام‌ها با تاریخ شمسی
-- SELECT TOP 50 id, name, contact, body, dbo.miladitoshamsifull(created) AS [تاریخ], device
-- FROM dbo.app_message ORDER BY id DESC;
