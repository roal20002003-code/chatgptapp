using System;
using System.Configuration;
using System.Data;
using System.Data.SqlClient;
using System.Web;
using System.Web.Caching;

namespace new_astronomy
{
    /// <summary>
    /// دریافت پیام از اپ اندروید (POST فرم) و ذخیره در جدول app_message
    /// خروجی: {"ok":true,"message":"..."}
    /// </summary>
    public class AppMessage : IHttpHandler
    {
        // نام connection string خودتان در Web.config را اینجا بگذارید
        private const string CONN_NAME = "ConnStr";

        public void ProcessRequest(HttpContext ctx)
        {
            ctx.Response.ContentType = "application/json";
            ctx.Response.ContentEncoding = System.Text.Encoding.UTF8;

            if (ctx.Request.HttpMethod != "POST") { Write(ctx, false, "POST only"); return; }

            // Unvalidated: تا متن‌هایی که < دارند خطای Request Validation ندهند
            var f = ctx.Request.Unvalidated.Form;
            string name = Clip(f["name"], 100);
            string contact = Clip(f["contact"], 150);
            string body = Clip(f["body"], 2000);
            string device = Clip(f["device"], 150);
            string version = Clip(f["version"], 20);

            if (body.Length < 3) { Write(ctx, false, "متن پیام خالی است"); return; }

            // جلوگیری از ارسال پشت سر هم: هر IP یک پیام در ۶۰ ثانیه
            string ip = ctx.Request.UserHostAddress ?? "";
            string key = "appmsg_" + ip;
            if (HttpRuntime.Cache[key] != null) { Write(ctx, false, "لطفاً یک دقیقه بعد دوباره تلاش کنید"); return; }
            HttpRuntime.Cache.Insert(key, 1, null, DateTime.Now.AddSeconds(60), Cache.NoSlidingExpiration);

            try
            {
                string cs = ConfigurationManager.ConnectionStrings[CONN_NAME].ConnectionString;
                using (var cn = new SqlConnection(cs))
                using (var cmd = new SqlCommand(
                    "INSERT INTO app_message (name, contact, body, device, app_version, ip, created) " +
                    "VALUES (@name, @contact, @body, @device, @ver, @ip, GETDATE())", cn))
                {
                    cmd.Parameters.Add("@name", SqlDbType.NVarChar, 100).Value = name;
                    cmd.Parameters.Add("@contact", SqlDbType.NVarChar, 150).Value = contact;
                    cmd.Parameters.Add("@body", SqlDbType.NVarChar, 2000).Value = body;
                    cmd.Parameters.Add("@device", SqlDbType.NVarChar, 150).Value = device;
                    cmd.Parameters.Add("@ver", SqlDbType.NVarChar, 20).Value = version;
                    cmd.Parameters.Add("@ip", SqlDbType.NVarChar, 50).Value = ip;
                    cn.Open();
                    cmd.ExecuteNonQuery();
                }
                Write(ctx, true, "پیام شما ثبت شد. ممنون!");
            }
            catch (Exception)
            {
                ctx.Response.StatusCode = 500;
                Write(ctx, false, "خطا در ثبت پیام؛ بعداً تلاش کنید");
            }
        }

        private static string Clip(string s, int n)
        {
            s = (s ?? "").Trim();
            return s.Length > n ? s.Substring(0, n) : s;
        }

        private static void Write(HttpContext c, bool ok, string msg)
        {
            c.Response.Write("{\"ok\":" + (ok ? "true" : "false") +
                             ",\"message\":\"" + HttpUtility.JavaScriptStringEncode(msg) + "\"}");
        }

        public bool IsReusable { get { return false; } }
    }
}
