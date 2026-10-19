package app.famtv.home;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Telugu panchangam (amanta) computed on the device: tithi, paksham, masam and samvatsaram.
 * Sun and Moon from Meeus, "Astronomical Algorithms" (ch. 25 and 47), sidereal positions with
 * the Lahiri ayanamsa. Accurate to a minute or two, the same spread as published panchangams.
 * Plain Java with no Android dependencies, so it can be tested on any machine.
 */
public final class Panchang {

    static final String[] TITHI = {
            "Padyami", "Vidiya", "Tadiya", "Chavithi", "Panchami", "Shashti", "Saptami", "Ashtami",
            "Navami", "Dashami", "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi"};
    static final String[] MASAM = {
            "Chaitra", "Vaishakha", "Jyeshtha", "Ashadha", "Shravana", "Bhadrapada", "Ashwayuja",
            "Karthika", "Margashira", "Pushya", "Magha", "Phalguna"};
    static final String[] SAMVATSARAM = {
            "Prabhava", "Vibhava", "Shukla", "Pramoduta", "Prajotpatti", "Angirasa", "Shrimukha",
            "Bhava", "Yuva", "Dhatu", "Ishvara", "Bahudhanya", "Pramadi", "Vikrama", "Vrusha",
            "Chitrabhanu", "Svabhanu", "Tarana", "Parthiva", "Vyaya", "Sarvajit", "Sarvadhari",
            "Virodhi", "Vikruti", "Khara", "Nandana", "Vijaya", "Jaya", "Manmatha", "Durmukhi",
            "Hevilambi", "Vilambi", "Vikari", "Sharvari", "Plava", "Shubhakruth", "Shobhakruth",
            "Krodhi", "Vishvavasu", "Parabhava", "Plavanga", "Kilaka", "Saumya", "Sadharana",
            "Virodhikruth", "Paridhavi", "Pramadicha", "Ananda", "Rakshasa", "Nala", "Pingala",
            "Kalayukti", "Siddharthi", "Raudri", "Durmathi", "Dundubhi", "Rudhirodgari",
            "Raktakshi", "Krodhana", "Akshaya"};

    static final String[] NAKSHATRAM = {
            "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Arudra", "Punarvasu",
            "Pushyami", "Ashlesha", "Makha", "Pubba", "Uttara", "Hasta", "Chitta", "Swati",
            "Vishakha", "Anuradha", "Jyeshtha", "Moola", "Purvashadha", "Uttarashadha", "Shravanam",
            "Dhanishtha", "Shatabhisham", "Purvabhadra", "Uttarabhadra", "Revati"};

    // Telugu script
    static final String[] TITHI_TE = {
            "పాడ్యమి", "విదియ", "తదియ", "చవితి", "పంచమి", "షష్ఠి", "సప్తమి", "అష్టమి",
            "నవమి", "దశమి", "ఏకాదశి", "ద్వాదశి", "త్రయోదశి", "చతుర్దశి"};
    static final String[] MASAM_TE = {
            "చైత్ర", "వైశాఖ", "జ్యేష్ఠ", "ఆషాఢ", "శ్రావణ", "భాద్రపద", "ఆశ్వయుజ",
            "కార్తీక", "మార్గశిర", "పుష్య", "మాఘ", "ఫాల్గుణ"};
    static final String[] NAKSHATRAM_TE = {
            "అశ్విని", "భరణి", "కృత్తిక", "రోహిణి", "మృగశిర", "ఆరుద్ర", "పునర్వసు",
            "పుష్యమి", "ఆశ్లేష", "మఖ", "పుబ్బ", "ఉత్తర", "హస్త", "చిత్త", "స్వాతి",
            "విశాఖ", "అనూరాధ", "జ్యేష్ఠ", "మూల", "పూర్వాషాఢ", "ఉత్తరాషాఢ", "శ్రవణం",
            "ధనిష్ఠ", "శతభిషం", "పూర్వాభాద్ర", "ఉత్తరాభాద్ర", "రేవతి"};
    static final String[] SAMVATSARAM_TE = {
            "ప్రభవ", "విభవ", "శుక్ల", "ప్రమోదూత", "ప్రజోత్పత్తి", "ఆంగీరస", "శ్రీముఖ",
            "భావ", "యువ", "ధాత", "ఈశ్వర", "బహుధాన్య", "ప్రమాది", "విక్రమ", "వృష",
            "చిత్రభాను", "స్వభాను", "తారణ", "పార్థివ", "వ్యయ", "సర్వజిత్", "సర్వధారి",
            "విరోధి", "వికృతి", "ఖర", "నందన", "విజయ", "జయ", "మన్మథ", "దుర్ముఖి",
            "హేవిళంబి", "విళంబి", "వికారి", "శార్వరి", "ప్లవ", "శుభకృత్", "శోభకృత్",
            "క్రోధి", "విశ్వావసు", "పరాభవ", "ప్లవంగ", "కీలక", "సౌమ్య", "సాధారణ",
            "విరోధికృత్", "పరీధావి", "ప్రమాదీచ", "ఆనంద", "రాక్షస", "నల", "పింగళ",
            "కాళయుక్తి", "సిద్ధార్థి", "రౌద్రి", "దుర్మతి", "దుందుభి", "రుధిరోద్గారి",
            "రక్తాక్షి", "క్రోధన", "అక్షయ"};

    public final int tithi;          // 1..30 (1-15 Shukla, 16-30 Krishna)
    public final long tithiEndsMs;   // when the current tithi ends
    public final int masam;          // 0 = Chaitra
    public final boolean adhika;     // leap month
    public final int samvatsaram;    // 0 = Prabhava
    public final int nakshatram;     // 0 = Ashwini
    public final long nakshatramEndsMs;

    private Panchang(int tithi, long tithiEndsMs, int masam, boolean adhika, int samvatsaram,
                     int nakshatram, long nakshatramEndsMs) {
        this.tithi = tithi;
        this.tithiEndsMs = tithiEndsMs;
        this.masam = masam;
        this.adhika = adhika;
        this.samvatsaram = samvatsaram;
        this.nakshatram = nakshatram;
        this.nakshatramEndsMs = nakshatramEndsMs;
    }

    public String tithiName() {
        if (tithi == 15) return "Pournami";
        if (tithi == 30) return "Amavasya";
        return TITHI[(tithi - 1) % 15];
    }

    public String paksham() { return tithi <= 15 ? "Shukla" : "Krishna"; }

    public String masamName() { return (adhika ? "Adhika " : "") + MASAM[masam]; }

    public String samvatsaramName() { return SAMVATSARAM[samvatsaram]; }

    public String nakshatramName() { return NAKSHATRAM[nakshatram]; }

    public String tithiTe() {
        if (tithi == 15) return "పౌర్ణమి";
        if (tithi == 30) return "అమావాస్య";
        return TITHI_TE[(tithi - 1) % 15];
    }

    public String pakshamTe() { return tithi <= 15 ? "శుక్ల" : "కృష్ణ"; }

    public String masamTe() { return (adhika ? "అధిక " : "") + MASAM_TE[masam]; }

    /** Season: each pair of lunar months from Chaitra (adhika masam shares its nija masam's). */
    static final String[] RUTHUVU = { "Vasanta", "Grishma", "Varsha", "Sharad", "Hemanta", "Shishira" };
    static final String[] RUTHUVU_TE = { "వసంత", "గ్రీష్మ", "వర్ష", "శరద్", "హేమంత", "శిశిర" };
    public String ruthuvuName() { return RUTHUVU[masam / 2]; }
    public String ruthuvuTe() { return RUTHUVU_TE[masam / 2]; }

    public String samvatsaramTe() { return SAMVATSARAM_TE[samvatsaram]; }

    public String nakshatramTe() { return NAKSHATRAM_TE[nakshatram]; }

    // ------------------------------------------------------------------ calculation

    public static Panchang at(long utcMs) {
        double jd = julianDay(utcMs);
        double e = elongation(jd);
        int tithi = (int) Math.floor(e / 12) + 1;
        double endJd = solveElongation(jd, tithi * 12.0);

        // Amanta month: named after the sidereal sign the Sun is in at the new moon that began it
        // (Sun in Meena -> Chaitra). No sankranti between two new moons means an adhika month.
        double newMoon = solveElongation(jd - e / 12.19, 0);
        double nextNewMoon = solveElongation(newMoon + 29.6, 360);
        int sign = (int) Math.floor(siderealSun(newMoon) / 30);
        int nextSign = (int) Math.floor(siderealSun(nextNewMoon) / 30);
        int masam = (sign + 1) % 12;
        boolean adhika = sign == nextSign;

        // The samvatsaram starts at Ugadi (Chaitra Shukla Padyami), around March/April.
        Calendar ist = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        ist.setTimeInMillis(utcMs);
        int year = ist.get(Calendar.YEAR);
        if (ist.get(Calendar.MONTH) <= Calendar.APRIL && masam >= 8) year--;
        int samvatsaram = Math.floorMod(year - 1987, 60);   // 1987-88 was Prabhava

        // Nakshatram: the Moon's sidereal position in one of 27 equal parts of 13°20'.
        double span = 360.0 / 27;
        double moon = siderealMoon(jd);
        int nak = (int) Math.floor(moon / span);
        double nakEnd = jd;
        for (int i = 0; i < 30; i++) {
            double diff = (nak + 1) * span - siderealMoon(nakEnd);
            diff = ((diff % 360) + 540) % 360 - 180;
            nakEnd += diff / 13.18;                 // mean lunar motion, degrees per day
            if (Math.abs(diff) < 1e-6) break;
        }

        return new Panchang(tithi, jdToMs(endJd), masam, adhika, samvatsaram, nak, jdToMs(nakEnd));
    }

    static double siderealMoon(double jd) {
        return norm(moonLongitude(jd) - lahiri(jd));
    }

    /** Moon minus Sun, 0..360 degrees. */
    static double elongation(double jd) {
        return norm(moonLongitude(jd) - sunLongitude(jd));
    }

    /** First time at/after ~guess when the elongation reaches target (0..360). */
    static double solveElongation(double guess, double target) {
        double jd = guess;
        for (int i = 0; i < 30; i++) {
            double diff = target - elongation(jd);
            diff = ((diff % 360) + 540) % 360 - 180;   // shortest way, -180..180
            jd += diff / 12.19;                       // mean relative motion, degrees per day
            if (Math.abs(diff) < 1e-6) break;
        }
        return jd;
    }

    static double siderealSun(double jd) {
        return norm(sunLongitude(jd) - lahiri(jd));
    }

    /** Lahiri ayanamsa: 23°51'11" at J2000, precessing 50.29" per year. */
    static double lahiri(double jd) {
        return 23.85306 + (jd - 2451545.0) / 365.25 * 50.2888 / 3600;
    }

    /** Apparent geometric Sun longitude (Meeus ch. 25, low precision; ~0.01°). */
    static double sunLongitude(double jd) {
        double t = (tt(jd) - 2451545.0) / 36525;
        double l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t;
        double m = Math.toRadians(357.52911 + 35999.05029 * t - 0.0001537 * t * t);
        double c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * Math.sin(m)
                + (0.019993 - 0.000101 * t) * Math.sin(2 * m) + 0.000289 * Math.sin(3 * m);
        return norm(l0 + c - 0.00569);   // minus aberration
    }

    // Meeus table 47.A: multiples of D, M, M', F and the sine coefficient (1e-6 degrees).
    private static final int[][] MOON = {
            {0, 0, 1, 0, 6288774}, {2, 0, -1, 0, 1274027}, {2, 0, 0, 0, 658314},
            {0, 0, 2, 0, 213618}, {0, 1, 0, 0, -185116}, {0, 0, 0, 2, -114332},
            {2, 0, -2, 0, 58793}, {2, -1, -1, 0, 57066}, {2, 0, 1, 0, 53322},
            {2, -1, 0, 0, 45758}, {0, 1, -1, 0, -40923}, {1, 0, 0, 0, -34720},
            {0, 1, 1, 0, -30383}, {2, 0, 0, -2, 15327}, {0, 0, 1, 2, -12528},
            {0, 0, 1, -2, 10980}, {4, 0, -1, 0, 10675}, {0, 0, 3, 0, 10034},
            {4, 0, -2, 0, 8548}, {2, 1, -1, 0, -7888}, {2, 1, 0, 0, -6766},
            {1, 0, -1, 0, -5163}, {1, 1, 0, 0, 4987}, {2, -1, 1, 0, 4036},
            {2, 0, 2, 0, 3994}, {4, 0, 0, 0, 3861}, {2, 0, -3, 0, 3665},
            {0, 1, -2, 0, -2689}, {2, 0, -1, 2, -2602}, {2, -1, -2, 0, 2390},
            {1, 0, 1, 0, -2348}, {2, -2, 0, 0, 2236}, {0, 1, 2, 0, -2120},
            {0, 2, 0, 0, -2069}, {2, -2, -1, 0, 2048}, {2, 0, 1, -2, -1773},
            {2, 0, 0, 2, -1595}, {4, -1, -1, 0, 1215}, {0, 0, 2, 2, -1110},
            {3, 0, -1, 0, -892}, {2, 1, 1, 0, -810}, {4, -1, -2, 0, 759},
            {0, 2, -1, 0, -713}, {2, 2, -1, 0, -700}, {2, 1, -2, 0, 691},
            {2, -1, 0, -2, 596}, {4, 0, 1, 0, 549}, {0, 0, 4, 0, 537},
            {4, -1, 0, 0, 520}, {1, 0, -2, 0, -487}, {2, 1, 0, -2, -399},
            {0, 0, 2, -2, -381}, {1, 1, 1, 0, 351}, {3, 0, -2, 0, -340},
            {4, 0, -3, 0, 330}, {2, -1, 2, 0, 327}, {0, 2, 1, 0, -323},
            {1, 1, -1, 0, 299}, {2, 0, 3, 0, 294}};

    /** Geometric Moon longitude (Meeus ch. 47; ~10 arcseconds). */
    static double moonLongitude(double jd) {
        double t = (tt(jd) - 2451545.0) / 36525;
        double lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t * t + t * t * t / 538841;
        double d = 297.8501921 + 445267.1114034 * t - 0.0018819 * t * t + t * t * t / 545868;
        double m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t * t;
        double mp = 134.9633964 + 477198.8675055 * t + 0.0087414 * t * t + t * t * t / 69699;
        double f = 93.2720950 + 483202.0175233 * t - 0.0036539 * t * t;
        double a1 = 119.75 + 131.849 * t, a2 = 53.09 + 479264.290 * t;
        double ecc = 1 - 0.002516 * t - 0.0000074 * t * t;
        double sum = 0;
        for (int[] r : MOON) {
            double arg = Math.toRadians(r[0] * d + r[1] * m + r[2] * mp + r[3] * f);
            double k = r[1] == 0 ? 1 : Math.abs(r[1]) == 1 ? ecc : ecc * ecc;
            sum += r[4] * k * Math.sin(arg);
        }
        sum += 3958 * Math.sin(Math.toRadians(a1)) + 1962 * Math.sin(Math.toRadians(lp - f))
                + 318 * Math.sin(Math.toRadians(a2));
        return norm(lp + sum / 1e6);
    }

    static double julianDay(long utcMs) { return utcMs / 86400000.0 + 2440587.5; }

    static long jdToMs(double jd) { return Math.round((jd - 2440587.5) * 86400000.0); }

    /** Terrestrial time: UT plus delta-T (~69 s in the 2020s). */
    static double tt(double jd) { return jd + 69.0 / 86400; }

    static double norm(double a) { return ((a % 360) + 360) % 360; }
}
