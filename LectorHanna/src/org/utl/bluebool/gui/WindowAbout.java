package org.utl.bluebool.gui;

import java.io.File;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 *
 * @author LiveGrios
 */
public class WindowAbout extends Stage
{
    @FXML WebView wvAbout;
    @FXML Button btnAceptar;
    
    Scene scene;
    
    FXMLLoader fxmll;
            
    public WindowAbout()
    {
        super();
    }
    
    public void initComponents() throws Exception
    {
        fxmll = new FXMLLoader(WindowAbout.class.getResource("window_about.fxml"));
        fxmll.setController(this);
        fxmll.load();
        scene = new Scene(fxmll.getRoot());
        setScene(scene);
        
        loadContent();
        
        focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue)
            {
                if (!newValue)
                    hide();
            }
        });
        
        btnAceptar.setOnAction(evt ->{hide();});
    }
    
    private void loadContent()
    {
        File f = new File("html/index.html");
        try
        {
            
            wvAbout.getEngine().load(f.toURI().toURL().toString());
        } 
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
}
