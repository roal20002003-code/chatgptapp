-- =============================================
--  جدول نسخهٔ اپ اندروید
-- =============================================
CREATE TABLE dbo.app_version (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    version      NVARCHAR(20)   NOT NULL,            -- مثل 1.0.8
    url          NVARCHAR(300)  NOT NULL,            -- فقط مسیر روی سایت: /app/spaceweather-1.0.8.apk
    force_update BIT            NOT NULL DEFAULT 1,  -- 1 = اجباری | 0 = دکمهٔ «بعداً» هم دارد
    notes        NVARCHAR(1000) NULL,                -- تغییرات این نسخه (در صفحهٔ بروزرسانی نمایش داده می‌شود)
    created      DATETIME       NOT NULL DEFAULT GETDATE()
);
GO

-- نسخهٔ فعلی
INSERT INTO dbo.app_version (version, url, force_update, notes)
VALUES (N'1.0.7', N'/app/spaceweather-1.0.7.apk', 0, N'نسخهٔ اول برنامه');
GO

-- =============================================
--  select برای appversion.aspx  (آخرین ردیف = آخرین نسخه)
--  خروجی: {"version":"1.0.7","url":"/app/spaceweather-1.0.7.apk","force":false,"notes":"..."}
-- =============================================
SELECT TOP 1
    version,
    url,
    force_update        AS [force],
    ISNULL(notes, N'')  AS notes
FROM dbo.app_version
ORDER BY id DESC
FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;
GO

-- =============================================
--  برای انتشار نسخهٔ جدید فقط یک ردیف اضافه کنید، مثلاً:
-- INSERT INTO dbo.app_version (version, url, force_update, notes)
-- VALUES (N'1.0.8', N'/app/spaceweather-1.0.8.apk', 1, N'اضافه شدن اعلان طوفان‌ها');
-- =============================================
