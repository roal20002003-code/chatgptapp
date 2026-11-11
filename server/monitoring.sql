-- monitoring.sql : همهٔ پارامترها با یک بار اجرا، خروجی JSON (SQL Server 2016+)
SELECT
    CONVERT(varchar(19), GETDATE(), 126) AS [server_time],
    JSON_QUERY((SELECT TOP 1 kp    AS [value], CONCAT(miladi, ' ', [time]) AS [time] FROM kp            ORDER BY id DESC FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)) AS [params.kp_index],
    JSON_QUERY((SELECT TOP 1 speed AS [value], CONCAT(miladi, ' ', [time]) AS [time] FROM windspeed     ORDER BY id DESC FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)) AS [params.solar_wind_speed_km_s],
    JSON_QUERY((SELECT TOP 1 suf   AS [value], CONCAT(miladi, ' ', [time]) AS [time] FROM suf           ORDER BY id DESC FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)) AS [params.f10_7_index],
    JSON_QUERY((SELECT TOP 1 total AS [value], CONCAT(miladi, ' ', [time]) AS [time] FROM bclass        ORDER BY id DESC FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)) AS [params.Radio Blackouts Storm],
    JSON_QUERY((SELECT TOP 1 bz    AS [value], CONCAT(miladi, ' ', [time]) AS [time] FROM btbz          ORDER BY id DESC FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)) AS [params.bz_nT],
    JSON_QUERY((SELECT TOP 1 bt    AS [value], CONCAT(miladi, ' ', [time]) AS [time] FROM btbz          ORDER BY id DESC FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)) AS [params.bt_nT],
    JSON_QUERY((SELECT TOP 1 temp  AS [value], CONCAT(miladi, ' ', [time]) AS [time] FROM protonandtemp ORDER BY id DESC FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)) AS [params.temp],
    JSON_QUERY((SELECT TOP 1 logg  AS [value], CONCAT(miladi, ' ', [time]) AS [time] FROM int_proton    ORDER BY id DESC FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)) AS [params.proton]
FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;
