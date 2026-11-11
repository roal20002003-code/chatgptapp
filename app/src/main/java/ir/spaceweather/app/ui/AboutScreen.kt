package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.spaceweather.app.data.Fa

/** متن‌ها از صفحهٔ «درباره ما» سایت (aboutus.aspx) */
@Composable
fun AboutScreen() {
    val ctx = LocalContext.current
    fun open(uri: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ---------- معرفی ----------
        item {
            Card {
                Text(tr("درباره ما", "About us"), color = C.sun, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(tr("پنجره‌ای رو به خورشید، برای همه", "A window on the Sun, for everyone"), color = C.text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                Body(
                    tr("spaceweather.ir وب‌سایت تخصصی مرکز رصد فعالیت‌های خورشیدی است. ما فعالیت خورشید را به‌طور پیوسته " +
                        "پایش و تحلیل می‌کنیم، پارامترهای مختلف خورشیدی و هواشناسی فضایی را جمع‌آوری می‌کنیم و هر ساعت " +
                        "گزارشی از وضعیت خورشید منتشر می‌کنیم؛ به زبان فارسی، ساده و قابل‌فهم برای همه.", "spaceweather.ir is the website of the Solar Activity Observation Center. We continuously monitor and analyse solar activity, collect solar and space weather parameters, and publish an hourly report on the state of the Sun — simple and easy to understand for everyone.")
                )
                Text(tr("مرکز رصد فعالیت‌های خورشیدی · مشهد", "Solar Activity Observation Center · Mashhad, Iran"), color = C.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            val stats = listOf(
                tr("ساعتی", "Hourly") to tr("گزارش وضعیت خورشید", "solar status report"),
                tr("۱۵ پارامتر", "15 parameters") to tr("با نمودار و توضیح کامل", "with charts and explanations"),
                tr("اخطار", "Alerts") to tr("هشدارهای هواشناسی فضایی", "space weather warnings"),
                tr("رایگان", "Free") to tr("API و دسترسی به داده‌ها", "API and data access"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (big, small) ->
                            Column(
                                Modifier.weight(1f).height(78.dp).clip(RoundedCornerShape(12.dp)).background(C.deep)
                                    .border(1.dp, C.line, RoundedCornerShape(12.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                            ) {
                                Text(big, color = C.sun, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(small, color = C.muted, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        // ---------- کار ما ----------
        item {
            Card {
                SectionTitle(tr("کار ما", "What we do"), tr("از داده‌های خام تا گزارشی که همه بفهمند", "From raw data to a report everyone can understand"))
                Body(tr("داده‌های فضاپیماها و رصدخانه‌ها را جمع‌آوری، بررسی و به نمودار، گزارش و هشدار تبدیل می‌کنیم.", "We collect and check data from spacecraft and observatories and turn it into charts, reports and alerts."))
                listOf(
                    tr("پایش و گزارش ساعتی", "Hourly monitoring and reports") to tr("وضعیت خورشید، باد خورشیدی و میدان مغناطیسی زمین هر ساعت به‌روز می‌شود و در قالب یک گزارش خوانا منتشر می‌شود.", "The state of the Sun, the solar wind and Earth’s magnetic field is updated every hour and published as a readable report."),
                    tr("نمودارهای تعاملی", "Interactive charts") to tr("پارامترهای طوفان خورشیدی به‌صورت نمودارهای هم‌محور و تعاملی نمایش داده می‌شوند، همراه با راهنمای تفسیر هر مقدار.", "Solar storm parameters are shown as interactive charts on a shared time axis, with a guide to interpreting each value."),
                    tr("اخطار و هشدار", "Alerts and warnings") to tr("طوفان‌های خورشیدی را بررسی و هشدارهای لازم را منتشر می‌کنیم تا از زیرساخت‌های حساس مانند ماهواره‌ها و شبکه‌های ارتباطی محافظت شود.", "We track solar storms and publish warnings to help protect sensitive infrastructure such as satellites and communication networks."),
                    tr("آرشیو و API رایگان", "Archive and free API") to tr("آرشیو تاریخی داده‌ها برای مطالعهٔ چرخه‌های بلندمدت خورشیدی در دسترس است و داده‌ها از طریق API رایگان به‌صورت JSON ارائه می‌شوند.", "A historical archive is available for studying long-term solar cycles, and the data is offered as JSON through a free API."),
                ).forEach { (t, d) -> Item(t, d) }
            }
        }

        // ---------- مأموریت ----------
        item {
            Card {
                SectionTitle(tr("مأموریت ما", "Our mission"))
                Body(
                    tr("خورشید روی زندگی روزمرهٔ ما اثر می‌گذارد؛ از ماهواره‌ها و سامانه‌های ناوبری گرفته تا ارتباطات رادیویی و " +
                        "شبکه‌های برق. هدف ما این است که داده‌های پیچیدهٔ هواشناسی فضایی را به اطلاعاتی دقیق، به‌موقع و " +
                        "قابل‌فهم به زبان فارسی تبدیل کنیم.", "The Sun affects our everyday lives — from satellites and navigation systems to radio communication and power grids. Our goal is to turn complex space weather data into accurate, timely and understandable information.")
                )
                Spacer(Modifier.height(8.dp))
                Body(
                    tr("ما به پیش‌بینی هواشناسی فضایی و پشتیبانی از عملیات ماهواره‌ای و سامانه‌های ارتباطی کمک می‌کنیم و با " +
                        "آرشیو تاریخی داده‌ها، امکان مطالعهٔ چرخه‌های بلندمدت خورشید و اثر آن‌ها بر اقلیم و فناوری را فراهم می‌کنیم.", "We support space weather forecasting and satellite and communication operations, and our historical archive makes it possible to study long-term solar cycles and their effects on climate and technology.")
                )
            }
        }

        // ---------- مخاطبان ----------
        item {
            Card {
                SectionTitle(tr("این سایت برای چه کسانی است؟", "Who is it for?"))
                listOf(
                    tr("🔭 پژوهشگران و دانشجویان", "🔭 Researchers and students") to tr("داده‌های ساعتی، آرشیو و نمودارهای قابل مقایسه برای مطالعه و پژوهش.", "Hourly data, an archive and comparable charts for study and research."),
                    tr("🛰️ ماهواره و ارتباطات", "🛰️ Satellites and communications") to tr("هشدارهای قطع رادیویی، طوفان تابشی و طوفان ژئومغناطیسی.", "Radio blackout, radiation storm and geomagnetic storm alerts."),
                    tr("📻 رادیوآماتورها", "📻 Radio amateurs") to tr("شار 10.7 سانتی‌متر، پرتو X و وضعیت یونوسفر برای ارتباطات HF.", "10.7 cm flux, X-rays and ionospheric conditions for HF communication."),
                    tr("✨ علاقه‌مندان به نجوم", "✨ Astronomy enthusiasts") to tr("تصاویر زندهٔ خورشید، اخبار و توضیحات ساده دربارهٔ پدیده‌های خورشیدی.", "Live images of the Sun, news and simple explanations of solar phenomena."),
                ).forEach { (t, d) -> Item(t, d) }
            }
        }

        // ---------- تیم ----------
        item {
            Card {
                SectionTitle(tr("تیم ما", "Our team"), tr("پشت این گزارش‌ها", "Behind these reports"))
                TEAM.forEach { m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = m.photo, contentDescription = m.name, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(C.deep).border(2.dp, C.sun, CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(m.name, color = C.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(m.role, color = C.muted, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Body(
                    tr("گزارش‌های ساعتی، اخبار و مقاله‌های این سایت زیر نظر علی سلطانی، منجم آماتور و علاقه‌مند به عکاسی از خورشید، " +
                        "تهیه و منتشر می‌شوند.", "The hourly reports, news and articles are prepared and published under the supervision of Ali Soltani, an amateur astronomer and solar photographer.")
                )
            }
        }

        // ---------- منابع داده ----------
        item {
            Card {
                SectionTitle(tr("منابع داده", "Data sources"), tr("داده‌های ما از کجا می‌آیند؟", "Where does our data come from?"))
                Body(tr("داده‌های خام از مراکز علمی و فضاپیماهای بین‌المللی دریافت و پس از بررسی، در قالب گزارش و نمودار ارائه می‌شوند.", "Raw data is received from international scientific centres and spacecraft and, after checking, presented as reports and charts."))
                Spacer(Modifier.height(6.dp))
                listOf(
                    "NOAA SWPC" to tr("شاخص‌ها، اخطارها و پیش‌بینی‌های هواشناسی فضایی", "Space weather indices, alerts and forecasts"),
                    "GOES" to tr("پرتو X، شار پروتون و الکترون", "X-rays, proton and electron flux"),
                    "DSCOVR · ACE" to tr("سرعت، چگالی و میدان مغناطیسی باد خورشیدی", "Solar wind speed, density and magnetic field"),
                    "NASA SDO" to tr("تصاویر زندهٔ خورشید در طول‌موج‌های مختلف", "Live images of the Sun at many wavelengths"),
                    "GFZ Potsdam" to tr("شاخص ژئومغناطیسی Kp", "Geomagnetic Kp index"),
                    "WDC Kyoto" to tr("شاخص Dst", "Dst index"),
                ).forEach { (src, d) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            src, color = C.sun, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.width(110.dp)
                        )
                        Text(d, color = C.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // ---------- تماس ----------
        item {
            Card {
                SectionTitle(tr("تماس با ما", "Contact us"), tr("با ما در ارتباط باشید", "Get in touch"))
                Contact(tr("ایمیل", "Email"), "info@spaceweather.ir") { open("mailto:info@spaceweather.ir") }
                Contact(tr("تلفن همراه", "Mobile"), Fa.ltr("0915 503 8821")) { open("tel:09155038821") }
                Contact(tr("تلفن", "Phone"), Fa.ltr("051-36076217")) { open("tel:05136076217") }
                Contact(tr("نشانی", "Address"), tr("مشهد، جنب ایستگاه راه‌آهن مطهری", "Next to Motahari railway station, Mashhad, Iran"), null)
                Spacer(Modifier.height(10.dp))
                Text(tr("همکاری، پیشنهاد یا گزارش خطا", "Collaboration, suggestions or bug reports"), color = C.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Body(
                    tr("اگر پژوهشگر هستید و به داده‌های ما نیاز دارید، پیشنهادی برای بهتر شدن سایت دارید یا خطایی در داده‌ها " +
                        "دیده‌اید، خوشحال می‌شویم از شما بشنویم.", "If you are a researcher who needs our data, have a suggestion for improving the site, or have spotted an error in the data, we would be glad to hear from you.")
                )
            }
        }
        item {
            Text(
                "spaceweather.ir", color = C.muted, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { open("http://spaceweather.ir/aboutus.aspx") }.padding(8.dp)
            )
        }
    }
}

@Composable
private fun Body(t: String) = Text(t, color = C.text, fontSize = 14.sp, lineHeight = 25.sp)

@Composable
private fun Item(title: String, desc: String) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(title, color = C.sun, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(desc, color = C.text, fontSize = 13.sp, lineHeight = 23.sp)
    }
}

@Composable
private fun Contact(label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = C.muted, fontSize = 13.sp, modifier = Modifier.width(90.dp))
        Text(value, color = if (onClick != null) C.sun else C.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}
