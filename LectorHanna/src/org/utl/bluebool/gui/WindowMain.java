package org.utl.bluebool.gui;

//  Import de librerías y clases
//  fazecast SerialPort es una librería que permite comunicarnos con los puertos seriales.
import com.fazecast.jSerialComm.SerialPort;

/*  Hansolo Medusa es una librería que nos permite crear "Gauges" o medidores para mostrar de manera atractiva información de mediciones en tiempo real 
    Se importan los aspectos de nuestros medidores. */
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.LcdDesign;
import eu.hansolo.medusa.skins.LcdSkin;
import eu.hansolo.medusa.skins.LinearSkin;
import eu.hansolo.medusa.skins.SimpleDigitalSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.image.Image;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.swing.JOptionPane;

/*  La librería hi2550 consta de todos los métodos desarrollados especialmente para realizar las funciones correspondientes a la lectura
    de datos provenientes del dispositivo 'HANNA HI2500' */
import org.utl.dsm.hi2550.ControllerHI2550;
import org.utl.dsm.hi2550.HI2550DataListener;
import org.utl.dsm.hi2550.model.HI2550Read;

/*
    - Clase que ejecuta el programa y los métodos correspondientes a las funciones asignadas a cada elemento que se muestra en la interfaz.
    - La aplicación que se ejecuta aquí corresponde a un programa que recibe y lee datos e información enviada desde un dispositivo externo
      llamado 'HANNA HI2500'. La información enviada desde HANNA corresponde a lecturas de temperatura, pH y conductividad de liquidos.
      Esta información debe ser mostrada en tiempo real mediante tres medidores y tres gráficas.
        - El medidor y la gráfica número uno muestran los datos de la lectura de la temperatura.
        - El medidor y la gráfica número dos muestran los datos de la lectura del pH.
        - El medidor y la gráfica número tres muestran los datos de la lectura de la conductividad.

    - Componentes de la interfaz:
        + Selector de puerto (para comunicación con el dispositivo)
        + Botón de actualización de puertos (en caso de que se encuentre activo más de un puerto)
        + Botón de conexión con el puerto.
        + Botón de desconexión con el puerto.
        + Tres botones correspondientes a la cantidad de decimales con los que se mostrarán los datos arrojados por la lectura (0.001, 0.01, 0.1)
        + Botón de cambio de resolución
        + Selector de frecuencia de lectura de los datos.
        + Botón para iniciar la lectura
        + Botón para detener la lectura
        + Botón para limpiar la configuración (reiniciar a por defecto)
        + Medidores y gráficas

    - Última fecha de modificación: 18 de marzo de 2023.
    - Propietarios del proyecto: Bluebool y Gil Rios.
 */
public class WindowMain extends Application {

    /*  Declaración de todos los elementos usados dentro de la construcción de la interfaz gráfica
        Mediante SceneBuilder */
    @FXML
    GridPane PaneCOM;
    @FXML
    ComboBox cmbPuertos;
    @FXML
    Button btnActualizarPuertos;
    @FXML
    Button btnConectar;
    @FXML
    Button btnDesonectar;
    @FXML
    Button btnPhRes00;
    @FXML
    Button btnPhRes01;
    @FXML
    Button btnPhRes02;
    @FXML
    Button btnIniciar;
    @FXML
    Button btnDetener;
    @FXML
    Button btnLimpiar;
    @FXML
    Button btnPrueba;
    
    @FXML
    Button btnAcercaDe;
    
    @FXML
    TextField txtFrecuencia;
//    @FXML
//    TextArea txtaSalida;
    @FXML
    Gauge gaugeTemp;
    @FXML
    Gauge gaugePH;
    @FXML
    Gauge gaugeCond;
    @FXML
    CategoryAxis ejeXTemp;
    @FXML
    NumberAxis ejeYTemp;
    @FXML
    LineChart<String, Number> chartTemp;
    @FXML
    CategoryAxis ejeXPh;
    @FXML
    NumberAxis ejeYPh;
    @FXML
    LineChart<String, Number> chartPh;
    @FXML
    CategoryAxis ejeXCond;
    @FXML
    NumberAxis ejeYCond;
    @FXML
    LineChart<String, Number> chartCond;
    @FXML
    Slider txtSlider;

    FXMLLoader fxmll;

    Stage window;
    Scene scene;
    
    WindowAbout windowAbout;

    ControllerHI2550 controladorHanna;

    int decimalesRes = 0;

    // Lista en donde se almacenarán las lecturas
    List<HI2550Read> lecturas;
    // Tamaño predeterminado que tendrá cada gráfica (sin mostrar información
    int WINDOW_SIZE = 50;

    /* Objeto que se encargará de dibujar los dos ejes y su contenido. Contiene 
        una lista de todos los datos que tendrá la trama*/
    XYChart.Series<String, Number> seriesTemp;
    XYChart.Series<String, Number> seriesPH;
    XYChart.Series<String, Number> seriesCond;

    /* Método que carga el archivo fxml para ser mostrado y añade la clase actual como controlador del archivo */
    public WindowMain() {
        super();
        fxmll = new FXMLLoader(WindowMain.class.getResource("window_main.fxml"));
        fxmll.setController(this);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Carga la escena con el archivo fxml al iniciar la aplicación
        fxmll.load();
        scene = new Scene(fxmll.getRoot());

        // Se ejecuta el método que inicializa ciertos componentes
        initComponents();

        /*  Se guarda la escena en una nueva pantalla y se agregan propiedades
            a esta pantalla para después mostrarla */
        window = primaryStage;
        window.setScene(scene);
        window.setTitle("Hanna HI2550 - Test COM");
        window.setMinWidth(1200);
        window.setMinHeight(700);
        window.setMaximized(true);
        window.show();
    }

    // Método que inicializa los componentes
    private void initComponents() throws Exception {
        
        actualizarPuertosCOM();

        // Desactiva los botones que ejecutan la mayoría de las acciones al iniciar la aplicación
        btnDesonectar.setDisable(true);
        btnIniciar.setDisable(true);
        btnDetener.setDisable(true);
        btnLimpiar.setDisable(false);
        btnPhRes00.setDisable(true);
        btnPhRes01.setDisable(true);
        btnPhRes02.setDisable(true);
        txtFrecuencia.setDisable(true);
        txtSlider.setDisable(true);
        
        windowAbout = new WindowAbout();
        windowAbout.initComponents();
        

        controladorHanna = new ControllerHI2550();
        
        // Se asignan las acciones que se realizarán al pulsar cada botón
        btnActualizarPuertos.setOnAction(evt -> {
            actualizarPuertosCOM();
        });
        btnConectar.setOnAction(evt -> {
            conectarConPuertoCOM();
        });
        btnDesonectar.setOnAction(evt -> {
            desconectarPuertoCOM();
        });
        btnPhRes00.setOnAction(evt -> {
            configurarResolucion(1);
        });
        btnPhRes01.setOnAction(evt -> {
            configurarResolucion(2);
        });
        btnPhRes02.setOnAction(evt -> {
            configurarResolucion(3);
        });
        btnIniciar.setOnAction(evt -> {
            iniciarLectura();
        });
        btnDetener.setOnAction(evt -> {
            detenerLectura();
        });
        btnLimpiar.setOnAction(evt -> {
            limpiarSeleccion();
        });
        btnPrueba.setOnAction(evt -> {
            pruebas();
        });
        
        btnAcercaDe.setOnAction(evt -> {
            mostrarAbout();
        });
        
        txtFrecuencia.setOnKeyReleased(evt -> {
            if (evt.getCode() == KeyCode.ENTER) {
                actualizarFrecuenciaTxt();
            }
        });
        txtSlider.setOnDragDetected(evt -> {
            actualizarFrecuenciaSlider();
        });
        
        

        // Se ejecutan los métodos que inicializan los medidores y los gráficos
        initTempGauge();
        initTempChart();
        initPHGauge();
        InitPHChart();
        initConductividadGauge();
        initConductividadChart();
    }

    // Método que obtiene los puertos de comunicación serial y actualiza la lista de puertos disponibles
    private void actualizarPuertosCOM() {
        try {
            cmbPuertos.getItems().clear();
            for (SerialPort sp : SerialPort.getCommPorts()) {
                cmbPuertos.getItems().add(sp.getDescriptivePortName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método que toma el puerto disponible seleccionado y conecta la aplicación con este puerto para poder ejecutar el resto de funciones
    private void conectarConPuertoCOM() {
        try {
            if (cmbPuertos.getSelectionModel().getSelectedIndex() < 0) {
                // En caso de que no haya ningún puerto seleccionado, se mostrará un mensaje al usuario que solicita seleccionar un puerto
                JOptionPane.showMessageDialog(null, "Selecciona un puerto de la lista para continuar :D", "Ola", 2);
                return;
            } else {
                // En caso de que se encuentre un puerto seleccionado, se activan los botones anteriormente desactivados.
                btnDesonectar.setDisable(false);
                btnConectar.setDisable(true);
                btnActualizarPuertos.setDisable(true);
                cmbPuertos.setDisable(true);
                btnIniciar.setDisable(false);
                btnDetener.setDisable(false);
                btnLimpiar.setDisable(false);
                btnPhRes00.setDisable(false);
                btnPhRes01.setDisable(false);
                btnPhRes02.setDisable(false);
                txtFrecuencia.setDisable(false);
                txtSlider.setDisable(false);

                // Se asigna el puerto seleccionado al controlador de nuestro dipositivo para iniciar la conexión con este
                controladorHanna.configure(cmbPuertos.getSelectionModel().getSelectedItem().toString());
                controladorHanna.open();
                //txtaSalida.setText("Conectado al Puerto Serie [" + cmbPuertos.getSelectionModel().getSelectedItem().toString() + "]");
            }
            // En caso de algún error se ejecuta una excepción
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método que cierra la conexión con el dispositivo HANNA, desconectando el puerto
    private void desconectarPuertoCOM() {
        try {
            PaneCOM.setDisable(false);
            btnDesonectar.setDisable(true);
            btnConectar.setDisable(false);
            cmbPuertos.setDisable(false);
            btnActualizarPuertos.setDisable(false);
            btnIniciar.setDisable(true);
            btnDetener.setDisable(true);
            btnLimpiar.setDisable(true);
            btnPhRes00.setDisable(true);
            btnPhRes01.setDisable(true);
            btnPhRes02.setDisable(true);
            txtFrecuencia.setDisable(true);
            txtSlider.setDisable(true);

            controladorHanna.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Método que configura la resolución con la que se tomarán los datos de lectura y con la que se mostrarán los datos en las gráficas 
       Esto se refiere a la cantidad de decimales que se mostrarán */
    private void configurarResolucion(int res) {
        try {
            switch (res) {
                case 1:
                    controladorHanna.executeSetup(ControllerHI2550.CommandSetup.CHR00);
                    decimalesRes = 3;
                    gaugeTemp.setDecimals(decimalesRes);
                    gaugePH.setDecimals(decimalesRes);
                    gaugeCond.setDecimals(decimalesRes);
                    break;
                case 2:
                    controladorHanna.executeSetup(ControllerHI2550.CommandSetup.CHR01);
                    decimalesRes = 2;
                    gaugeTemp.setDecimals(decimalesRes);
                    gaugePH.setDecimals(decimalesRes);
                    gaugeCond.setDecimals(decimalesRes);
                    break;
                case 3:
                    controladorHanna.executeSetup(ControllerHI2550.CommandSetup.CHR02);
                    decimalesRes = 1;
                    gaugeTemp.setDecimals(decimalesRes);
                    gaugePH.setDecimals(decimalesRes);
                    gaugeCond.setDecimals(decimalesRes);
                    break;
            }
            // En caso de haber algún error, se ejecuta una excepción
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método que inicia la lectura para tomar la información que envía el dispositivo.
    private void iniciarLectura() {

        System.out.println("Hola, vamos a leer los datos");

        /*  Sentencia if que almacena las lecturas arrojadas por el dispositivo HANNA en un ArrayList.
            De igual manera inicializa los objetos de tipo XYChart para añadirlos a los gráficos */
        if (lecturas == null) {
            lecturas = new ArrayList<>();

            seriesTemp = new XYChart.Series<>();
            chartTemp.getData().addAll(seriesTemp);

            seriesPH = new XYChart.Series<>();
            chartPh.getData().addAll(seriesPH);

            seriesCond = new XYChart.Series<>();
            chartCond.getData().addAll(seriesCond);
        } else  {
            lecturas.clear();
            lecturas = null;

            seriesTemp.getData().clear();
            seriesPH.getData().clear();
            seriesCond.getData().clear();

            chartTemp.getData().clear();
            chartPh.getData().clear();
            chartCond.getData().clear();
        }

        // Se crea un objeto de tipo listener para saber cuandos se han leído datos.
        HI2550DataListener listener = new HI2550DataListener() {
            @Override
            public void setOnDataAvailable(HI2550Read hir, boolean inReadingThread) {
                //txtaSalida.setText(txtaSalida.getText() + hir.toString() + "\n");
                //System.out.println(txtaSalida.getText() + hir.toString() + "\n");

                lecturas.add(hir);

                // Se van agregando a las gráficas los datos tomados por la lectura
                seriesTemp.getData().add(new XYChart.Data("" + lecturas.size(), hir.getTemperature()));
                seriesPH.getData().add(new XYChart.Data("" + lecturas.size(), hir.getPh()));
                seriesCond.getData().add(new XYChart.Data("" + lecturas.size(), hir.getMv()));

                // Se va actualizando el valor de los medidores(gauges) conforme avanza la lectura
                gaugeTemp.setValue(hir.getTemperature());
                gaugePH.setValue(hir.getPh());
                gaugeCond.setValue(hir.getMv());

            }
        };
        /*  TryCatch que asigna la frecuencia de lectura y el listener al controlador y se inicia la lectura.
            En caso de haber algún error se ejecuta una excepción */
        try {
            btnLimpiar.setDisable(true);
            btnPhRes00.setDisable(true);
            btnPhRes01.setDisable(true);
            btnPhRes02.setDisable(true);
            btnDesonectar.setDisable(true);
            btnIniciar.setDisable(true);
            txtFrecuencia.setDisable(true);
            txtSlider.setDisable(true);
            btnDetener.setDisable(false);

            controladorHanna.setFrequencyRead(1800);
            controladorHanna.setHi2550DataListener(listener);
            controladorHanna.startRead();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* método que detiene la lectura */
    private void detenerLectura() {
        controladorHanna.stopRead();
        btnIniciar.setDisable(false);
        btnLimpiar.setDisable(false);
        btnPhRes00.setDisable(false);
        btnPhRes01.setDisable(false);
        btnPhRes02.setDisable(false);
        btnDesonectar.setDisable(false);
        btnIniciar.setDisable(false);
        txtFrecuencia.setDisable(false);
        txtSlider.setDisable(false);
        btnDetener.setDisable(true);
    }

    private void limpiarSeleccion() {
        txtFrecuencia.setText("1");
        txtSlider.adjustValue(1);
        
        if (lecturas != null) {
            lecturas.clear();
            lecturas = null;
            
            seriesTemp.getData().clear();
            seriesPH.getData().clear();
            seriesCond.getData().clear();

            chartTemp.getData().clear();
            chartPh.getData().clear();
            chartCond.getData().clear();
        }
        
        seriesTemp.getData().clear();
        seriesPH.getData().clear();
        seriesCond.getData().clear();

        chartTemp.getData().clear();
        chartPh.getData().clear();
        chartCond.getData().clear();
        
        gaugeCond.setValue(0);
        gaugeTemp.setValue(0);
        gaugePH.setValue(0);
    }

    /* Método que actualiza la frecuencia de lectura mediante el valor asignado por el usuario.
        En caso de haber algún error, se ejecuta una excepción*/
    private void actualizarFrecuenciaTxt() {
        int nuevaFrecuencia = 1;
        try {
            nuevaFrecuencia = Integer.valueOf(txtFrecuencia.getText().trim());
            txtSlider.adjustValue(Double.parseDouble(txtFrecuencia.getText()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            controladorHanna.setFrequencyRead(nuevaFrecuencia);
        }
    }

    private void actualizarFrecuenciaSlider() {
        int nuevaFrecuencia = 1;
        try {
            nuevaFrecuencia = (int) txtSlider.getValue();
            txtFrecuencia.setText(String.valueOf(nuevaFrecuencia));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            controladorHanna.setFrequencyRead(nuevaFrecuencia);
        }
    }

    // Método que crea el medidor o 'gauge' correspondiente a la temperatura
    private void initTempGauge() {
        // Se le añade un aspecto linear, se le asigna un título, unidad y el color de esta
        gaugeTemp.setSkin(new LinearSkin(gaugeTemp));
        gaugeTemp.setTitle("Temperatura");
        gaugeTemp.setUnit("C°");
        gaugeTemp.setUnitColor(Color.CRIMSON);
        gaugeTemp.setValue(00.00); // posición por defecto en el gauge
        gaugeTemp.setAnimated(true);

        // Se le añaden propiedades y colores a otros elementos del medidor para volverlo más atractivo.
        gaugeTemp.startFromZeroProperty();
        gaugeTemp.setValueColor(Color.BLACK);
        gaugeTemp.setBarColor(Color.rgb(154, 214, 215));
        gaugeTemp.setNeedleColor(Color.RED);
        gaugeTemp.setThresholdColor(Color.rgb(132, 28, 38));
        gaugeTemp.setThreshold(10.00);
        gaugeTemp.setThresholdVisible(true);
        gaugeTemp.setTickLabelColor(Color.rgb(151, 151, 151));
        gaugeTemp.setTickMarkColor(Color.SILVER);
        gaugeTemp.setMinValue(0.00);
        gaugeTemp.setMaxValue(60.00);
        gaugeTemp.setMaxSize(500.00, 150.00);
    }

    // Método que crea el gráfico correspondiente a la temperatura
    private void initTempChart() {

        // Se añade una etiqueta al eje X y se desactiva la animación
        ejeXTemp.setLabel("Tiempo");
        ejeXTemp.setAnimated(true);

        // Se añade una etiqueta al eje Y y se activa la animación
        ejeYTemp.setLabel("C°");
        ejeYTemp.setAnimated(true);

        // Se añade una el nombre de la gráfica y se activa la animación
        chartTemp.setTitle("Temperatura");
        chartTemp.setAnimated(true);

        chartTemp.setCreateSymbols(false);
        chartTemp.setLegendVisible(false);
        
        chartTemp.setMinSize(400, 200);
        chartTemp.setPrefSize(700, 200);
    }

    // Método que crea el medidor o 'gauge' correspondiente al pH
    private void initPHGauge() {
        // Se le añade un aspecto de medidor digital, se le asigna un título, unidad y el color de esta
        gaugePH.setSkin(new SimpleDigitalSkin(gaugePH));
        gaugePH.setTitle("pH");
        gaugePH.setValue(00.00); // Posición por defecto en el gauge
        gaugePH.setAnimated(true);

        // Se le añaden propiedades y colores a otros elementos del medidor para volverlo más atractivo.
        gaugePH.returnToZeroProperty();
        gaugePH.startFromZeroProperty();
        gaugePH.setValueColor(Color.SILVER);
        gaugePH.setThresholdVisible(true);
        gaugePH.setMinValue(00.00);
        gaugePH.setMaxValue(14.00);
        gaugePH.setThresholdColor(Color.DARKRED);
        gaugePH.setThreshold(01.00);
        gaugePH.setMaxSize(500.00, 250.00);

    }

    // Método que crea el gráfico correspondiente al pH
    private void InitPHChart() {

        // Se le añade una etiqueta al eje X y se desactiva la animación
        ejeXPh.setLabel("Tiempo");
        ejeXPh.setAnimated(true);

        // Se le añade una etiqueta al eje Y y se activa la animación
        ejeYPh.setLabel("Nivel de PH");
        ejeYPh.setAnimated(true);

        // Se añaden los ejes al lineChart y se le asigna un nombre a la gráfica
        chartPh.setTitle("pH");
        chartPh.setAnimated(true);

        chartPh.setCreateSymbols(false);
        chartPh.setLegendVisible(false);
    }

    // Método que crea el medidor o 'gauge' correspondiente a la conductividad
    private void initConductividadGauge() {
        // Se le añade un aspecto de pantalla Lcd, se le asigna un título, unidad y el color de esta
        gaugeCond.setSkin(new LcdSkin(gaugeCond));
        gaugeCond.setTitle("mV");
        gaugeCond.setValue(00.00);
        gaugeCond.setAnimated(true);

        // Se le añaden propiedades y colores a otros elementos del medidor para volverlo más atractivo.
        gaugeCond.setMinValue(00.00);
        gaugeCond.setMaxValue(10.00);
        gaugeCond.setAutoScale(false);
        gaugeCond.setLcdCrystalEnabled(true);
        gaugeCond.setLcdDesign(LcdDesign.BLACK_YELLOW);
        gaugeCond.setPrefSize(400, 200);
        gaugeCond.setMaxSize(400, 200);

    }

    // Método que crea el gráfico correspondiente a la Conductividad
    private void initConductividadChart() {
        // Se le añade una etiqueta al eje X y se desactiva la animación
        ejeXCond.setLabel("Tiempo");
        ejeXCond.setAnimated(true);

        // Se le añade una etiqueta al eje Y y se activa la animación
        ejeYCond.setLabel("Mv");
        ejeYCond.setAnimated(true);

        // Se añaden los ejes al lineChart y se le asigna un nombre a la gráfica
        chartCond.setTitle("conductividad");
        chartCond.setAnimated(true);

        chartCond.setCreateSymbols(false);
        chartCond.setLegendVisible(false);
    }

    private void pruebas() {
        pruebaGauges();
        pruebaCharts();
    }

    private void pruebaCharts() {
        
        Double[] datosTemp = {1.54, 6.36, 14.23, 26.12, 37.32, 51.00, 30.45, 46.32, 50.12, 60.30, 70.10};
        Double[] datosPh = {2.0, 4.2, 5.7, 7.4, 5.32, 8.16, 6.93, 5.3, 2.5, 6.8, 9.7};
        Double[] datosCond = {1.23, 1.56, 1.82, 2.53, 2.67, 3.21, 1.97, 1.53, 2.34, 3.86, 4.56, 6.43};

        seriesTemp = new XYChart.Series<>();
        chartTemp.getData().addAll(seriesTemp);

        seriesPH = new XYChart.Series<>();
        chartPh.getData().addAll(seriesPH);

        seriesCond = new XYChart.Series<>();
        chartCond.getData().addAll(seriesCond);

        final int finalIndex = 0;
        int frecuencia = (Integer.valueOf(txtFrecuencia.getText().trim()) * 1000);

        Timer timerCharts = new Timer();
        timerCharts.scheduleAtFixedRate(new TimerTask() {
            int index = finalIndex;

            @Override
            public void run() {
                Platform.runLater(() ->{
                    if (index < datosTemp.length && index < datosPh.length && index < datosCond.length) {
                        seriesTemp.getData().add(new XYChart.Data<>("" + index, datosTemp[index]));
                        seriesPH.getData().add(new XYChart.Data<>("" + index, datosPh[index]));
                        seriesCond.getData().add(new XYChart.Data<>("" + index, datosCond[index]));

                        index++;
                    } else {
                        
                        System.out.println("se detendrá la lectura");
                        
//                        seriesTemp.getData().clear();
//                        seriesPH.getData().clear();
//                        seriesCond.getData().clear();
//
//                        chartTemp.getData().clear();
//                        chartPh.getData().clear();
//                        chartCond.getData().clear();
                        
                        timerCharts.cancel();
                        timerCharts.purge();
                    }

                    if (seriesTemp.getData().size() > 15) {
                        seriesTemp.getData().remove(0);
                        seriesPH.getData().remove(0);
                        seriesCond.getData().remove(0);
                    }
                    
                });
            }
        }, 0, 1000);
    }

    private void pruebaGauges() {

        Double[] datosTemp = {1.54, 6.36, 14.23, 26.12, 37.32, 51.00, 30.45, 46.32, 50.12, 60.30, 70.10};
        Double[] datosPh = {2.0, 4.2, 5.7, 7.4, 5.32, 8.16, 6.93, 5.3, 2.5, 6.8, 9.7};
        Double[] datosCond = {1.23, 1.56, 1.82, 2.53, 2.67, 3.21, 1.97, 1.53, 2.34, 3.86, 4.56, 6.43};

        final int finalIndex = 0;
        int frecuencia = (Integer.valueOf(txtFrecuencia.getText().trim()) * 1000);

        Timer timergauges = new Timer();
        timergauges.scheduleAtFixedRate(new TimerTask() {
            int index = finalIndex;

            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (index < Math.min(datosTemp.length, Math.min(datosPh.length, datosCond.length))) {
                        gaugeTemp.setValue(datosTemp[index]);
                        gaugePH.setValue(datosPh[index]);
                        gaugeCond.setValue(datosCond[index]);

                        index++;
                    } else {
                        System.out.println("Se detendrá la lectura de los medidores");
                        gaugeTemp.setValue(0);
                        gaugePH.setValue(0);
                        gaugeCond.setValue(0);
                        timergauges.cancel();
                        timergauges.purge();
                    }
                });
            }
        }, 0, 1000);

    }
    
     /**
     * Muestra la informacion el equipo de desarrollo
     */
    private void mostrarAbout() {
        try 
        {
            windowAbout.show();
        }
        catch (Exception e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR, "Error al mostrar la ventana Acerca De");
            alerta.show();
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
