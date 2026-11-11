using System;
using System.Configuration;
using System.Data.SqlClient;
using System.Text;
using System.Web;
using System.Web.Caching;

namespace new_astronomy
{
    /// <summary>
    /// اطلاعات آخرین نسخهٔ اپ اندروید به‌صورت JSON
    /// https://spaceweather.ir/appversion.aspx
    /// </summary>
    public partial class appversion : System.Web.UI.Page
    {
        // نام connection string خودتان در Web.config
        private const string CONN_NAME = "ConnStr";
        private const string CACHE_KEY = "appversion_json";

        private const string SQL =
            "SELECT TOP 1 version, url, force_update AS [force], ISNULL(notes, N'') AS notes " +
            "FROM dbo.app_version ORDER BY id DESC " +
            "FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;";

        protected void Page_Load(object sender, EventArgs e)
        {
            Response.Clear();
            Response.ContentType = "application/json";
            Response.ContentEncoding = Encoding.UTF8;
            Response.Cache.SetCacheability(HttpCacheability.NoCache);

            string json = HttpRuntime.Cache[CACHE_KEY] as string;
            if (json == null)
            {
                try
                {
                    var sb = new StringBuilder();
                    string cs = ConfigurationManager.ConnectionStrings[CONN_NAME].ConnectionString;
                    using (var cn = new SqlConnection(cs))
                    using (var cmd = new SqlCommand(SQL, cn))
                    {
                        cn.Open();
                        using (var rd = cmd.ExecuteReader())
                        {
                            // FOR JSON ممکن است خروجی را در چند ردیف بشکند
                            while (rd.Read()) sb.Append(rd.GetString(0));
                        }
                    }
                    json = sb.Length > 0 ? sb.ToString() : "{}";
                    // ۱ دقیقه کش: بعد از اضافه کردن ردیف جدید، حداکثر ۱ دقیقه طول می‌کشد تا دیده شود
                    HttpRuntime.Cache.Insert(CACHE_KEY, json, null, DateTime.Now.AddMinutes(1), Cache.NoSlidingExpiration);
                }
                catch (Exception)
                {
                    Response.StatusCode = 500;
                    json = "{\"error\":\"version unavailable\"}";
                }
            }

            Response.Write(json);
            Context.ApplicationInstance.CompleteRequest();
        }
    }
}
