package app.famtv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * The home background: an optional photo (darkened so text and tiles stay readable), with a soft
 * colour glow on top that follows the focused app.
 */
public class AmbientView extends View {

    int color = 0xFF202030;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint photoPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint scrim = new Paint();
    private Bitmap photo;
    private long photoStamp;

    public AmbientView(Context c) {
        super(c);
    }

    void setColor(int c) {
        color = c;
        invalidate();
    }

    /** Load (or drop) the background photo; only re-decodes when the file actually changed. */
    void loadPhoto(java.io.File f) {
        long stamp = f.exists() ? f.lastModified() + f.length() : 0;
        if (stamp == photoStamp) return;
        photoStamp = stamp;
        photo = null;
        if (stamp != 0) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            o.inSampleSize = 1;
            while (o.outWidth / (o.inSampleSize * 2) >= 1920) o.inSampleSize *= 2;
            o.inJustDecodeBounds = false;
            o.inPreferredConfig = Bitmap.Config.RGB_565;   // half the memory; fine for a dimmed photo
            photo = BitmapFactory.decodeFile(f.getPath(), o);
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w == 0) return;
        if (photo != null) {
            // centre-crop the photo to the screen
            float scale = Math.max(w / (float) photo.getWidth(), h / (float) photo.getHeight());
            int cw = Math.round(w / scale), ch = Math.round(h / scale);
            int cx = (photo.getWidth() - cw) / 2, cy = (photo.getHeight() - ch) / 2;
            canvas.drawBitmap(photo, new Rect(cx, cy, cx + cw, cy + ch), new Rect(0, 0, w, h), photoPaint);
            scrim.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{0xA6000000, 0x66000000, 0xBF000000, 0xEB000000},
                    new float[]{0f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, scrim);
        }
        int glow = (color & 0x00FFFFFF) | (photo != null ? 0x4D000000 : 0x8C000000);
        paint.setShader(new RadialGradient(w * 0.22f, h * 0.18f, w * 0.75f,
                new int[]{glow, (color & 0x00FFFFFF) | 0x26000000, 0x00000000},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
    }

    /** Average colour of a drawable, pushed toward a saturated mid-dark tone. */
    static int dominant(Drawable d) {
        try {
            Bitmap b = Bitmap.createBitmap(16, 9, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            // Sample a copy: the tile shares the original, and changing its bounds would shrink it.
            Drawable copy = d.getConstantState() != null ? d.getConstantState().newDrawable().mutate() : d;
            copy.setBounds(0, 0, 16, 9);
            copy.draw(c);
            long r = 0, g = 0, bl = 0, n = 0;
            float[] hsv = new float[3];
            for (int y = 0; y < 9; y++) for (int x = 0; x < 16; x++) {
                int p = b.getPixel(x, y);
                Color.colorToHSV(p, hsv);
                int weight = 1 + (int) (hsv[1] * 4);   // favour colourful pixels over white/black
                r += Color.red(p) * weight; g += Color.green(p) * weight; bl += Color.blue(p) * weight;
                n += weight;
            }
            Color.colorToHSV(Color.rgb((int) (r / n), (int) (g / n), (int) (bl / n)), hsv);
            hsv[1] = Math.min(1f, hsv[1] * 1.4f);
            hsv[2] = 0.6f;
            return Color.HSVToColor(hsv);
        } catch (Exception e) {
            return 0xFF202030;
        }
    }
}
