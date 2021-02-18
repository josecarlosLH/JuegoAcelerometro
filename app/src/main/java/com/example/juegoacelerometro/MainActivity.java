package com.example.juegoacelerometro;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    public float ancho, largo;
    private Point posicionBola;
    float senX, senY;
    SensorManager sensorManager;
    Sensor acelerometro;
    private Bitmap bola;
    private CanvasView view;

    public float getAncho() {
        return ancho;
    }

    public void setAncho(float ancho) {
        this.ancho = ancho;
    }

    public float getLargo() {
        return largo;
    }

    public void setLargo(float largo) {
        this.largo = largo;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        //Resolución de la pantalla
        Point resolution = new Point();
        getDisplay().getSize(resolution);
        setAncho(resolution.x);
        setLargo(resolution.y);

        //Se establece la view en la que vamos a trabajar
        view = new CanvasView(this);
        setContentView(view);

        //Inicializar sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        acelerometro = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            //Se recogen los valores del acelerómetro
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                senX = event.values[0];
                //Se controlan las pequeñas variaciones del acelerómetro para que cuando el dispositivo esté encima de la mesa, la bola no se mueva
                if (senX >= -0.4 && senX <= 0.4) {
                    senX = 0;
                }

                senY = event.values[1];
                //Se controlan las pequeñas variaciones del acelerómetro para que cuando el dispositivo esté encima de la mesa, la bola no se mueva
                if (senY >= -0.4 && senY <= 0.4) {
                    senY = 0;
                }
            }

            //Se van cambiando las coordenadas de la bola en función de los valores que se recojan en el acelerómetro
            posicionBola.x = (int) (posicionBola.x - (senX * 10));
            posicionBola.y = (int) (posicionBola.y + (senY * 10));

            view.reDraw();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    public class CanvasView extends View {

        public CanvasView(Context context) {
            super(context);

            //Se dibuja la bola
            Resources resources = getResources();
            bola = BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ball
            );
            bola = getResizedBitmap(bola, 150, 150);

            //Posicionamos la bola en el centro
            posicionBola = new Point();
            posicionBola.x = (int) ancho / 2 - (bola.getWidth() / 2);
            posicionBola.y = (int) largo / 2 - (bola.getHeight() / 2);
        }

        public Bitmap getResizedBitmap(@org.jetbrains.annotations.NotNull Bitmap bm, int nuevaAnchura, int nuevaAltura) {
            int anchoBola = bm.getWidth();
            int altoBola = bm.getHeight();
            float escalaDelAncho = ((float) nuevaAnchura) / anchoBola;
            float escalaDelLargo = ((float) nuevaAltura) / altoBola;

            // Se crea una matriz
            Matrix matrix = new Matrix();

            // Se redimensiona el bitmap haciendo uso de la matriz
            matrix.postScale(escalaDelAncho, escalaDelLargo);

            // Se dibuja el nuevo bitmap
            Bitmap resizedBitmap = Bitmap.createBitmap(
                    bm, 0, 0, anchoBola, altoBola, matrix, false);
            bm.recycle();
            return resizedBitmap;
        }

        //Este es el cuadro de diálogo que salta cuando el usuario gana
        public void alertDialog (View v) {
            AlertDialog.Builder alertDialogBu = new AlertDialog.Builder(getContext());
            alertDialogBu.setTitle("¡Has ganado!");
            alertDialogBu.setMessage("Suficiente juego por hoy");
            alertDialogBu.setIcon(R.drawable.ball);

            alertDialogBu.setPositiveButton( "Aceptar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finishAndRemoveTask();
                }
            });

            AlertDialog alertDialog = alertDialogBu.create();
            alertDialog.show();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            //Establece el fondo del canvas
            canvas.drawColor(Color.GRAY);

            //Se muestran los valores del acelerómetro
            Paint ejes = new Paint();
            ejes.setColor(Color.RED);
            ejes.setTextSize(60);

            canvas.drawText("X: " + String.valueOf(senX), canvas.getWidth()/3, canvas.getHeight()/2,ejes);
            canvas.drawText("Y: " + String.valueOf(senY), canvas.getWidth()/3, canvas.getHeight()/2+120,ejes);

            //Declara el pincel con el que pintamos los bordes
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(50);

            //Pintamos los bordes
            canvas.drawLine(0, largo, ancho, largo, paint); //abajo
            canvas.drawLine(ancho, 0, ancho, largo, paint); //derecha
            canvas.drawLine(0, 0, ancho, 0, paint); //arriba
            canvas.drawLine(0, 0, 0, largo, paint); //izquierda

            //Se declara el pincel nuevo de la línea que tiene que tocar la pelota
            Paint paintLinea = new Paint();
            paintLinea.setColor(Color.RED);
            paintLinea.setStrokeWidth(50);
            canvas.drawLine(ancho /4, largo, ancho /2, largo, paintLinea);//línea por la que se mete la pelota

            //Establecemos los límites por los que se puede mover la bola
            int resXBordeDerecho = (int) ((int) ancho - paint.getStrokeWidth() / 2);
            int resYconborde = (int) ((int) largo - paint.getStrokeWidth() / 2);

            //Esta es la condición por la que el jugador ganaría la partida
            if (posicionBola.x >= ancho /4 && posicionBola.x  <= ancho /2 - bola.getWidth()) {
                if (posicionBola.y > largo - bola.getHeight()) {
                    alertDialog(view);
                }
            //Aquí establecemos los límites de los bordes
            } else {
                if (posicionBola.x + bola.getWidth() > resXBordeDerecho) { //borde derecho
                    posicionBola.x = resXBordeDerecho - bola.getWidth() ;
                } else if (posicionBola.x <= paint.getStrokeWidth()) { //borde izquierdo
                    posicionBola.x = (int) paint.getStrokeWidth() / 2;
                }

                if (posicionBola.y + bola.getHeight() > resYconborde) { //borde superior
                    posicionBola.y = resYconborde - bola.getHeight() ;
                } else if (posicionBola.y <= paint.getStrokeWidth()) { //borde inferior
                    posicionBola.y = (int) paint.getStrokeWidth() / 2;
                }
            }

            canvas.drawBitmap(bola, posicionBola.x, posicionBola.y, null);
        }

        public void reDraw() {
            invalidate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, acelerometro,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}