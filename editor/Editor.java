package editor;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Application;
import javafx.stage.Stage;
import java.util.LinkedList;
import java.util.Iterator;
import javafx.scene.input.MouseEvent;
import java.lang.Math;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ScrollBar;
import javafx.geometry.Orientation;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.application.Application.Parameters;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;



public class Editor extends Application {
    private final Rectangle cursor;
    public TextStorage text;
    public int windowWidth;
    public int windowHeight;
    public ScrollBar scrollBar;
    public String fileName;
    public File file;
    public Group root;
    public boolean debug = false;
    public static final int MARGIN = 5;
    public static final int MARGINRIGHT = 5;

    public Editor() {
        cursor = new Rectangle(0, 0);

        
    }

	private class KeyEventHandler implements EventHandler<KeyEvent> {
        int textCenterX;
        int textCenterY;
        int cursorX;
        public int fontSize = 12;
        

        private String fontName = "Verdana";

        public KeyEventHandler(final Group roott, int windowWidth, int windowHeight) {
            fileName = getParameters().getRaw().get(0);
            file = new File(fileName);
            textCenterX = 5;
            textCenterY = 0;
            int cursorX = 0;
            root = roott;
            if (!file.exists()) {
                text = new TextStorage(windowWidth, windowHeight, fontSize, fontName, MARGIN, MARGINRIGHT);
            } else {
                text = new TextStorage(windowWidth, windowHeight, fontSize, fontName, file, MARGIN, MARGINRIGHT);
                renderAll();
            }
            if (getParameters().getRaw().size() > 1 && getParameters().getRaw().get(1).equals("debug")){
                debug = true;
                text.debug = true;
            }
            
        }


        @Override
        public void handle(KeyEvent keyEvent) {

            if (keyEvent.getEventType() == KeyEvent.KEY_TYPED) {
                String characterTyped = keyEvent.getCharacter();
                if (characterTyped.length() == 0 && text.size() != 0){
                    root.getChildren().remove(text.removeCurr());
                    rerenderAndUpdateBoundingBox();
                    snapScrollBar();
                    keyEvent.consume();
                	
                } else if (characterTyped.length() > 0 && !keyEvent.isShortcutDown() && keyEvent.getCharacter().charAt(0) != '\r') {
                    text.addAtCurr(characterTyped);
                    root.getChildren().add(text.peekCurr());
                    rerenderAndUpdateBoundingBox();
                    snapScrollBar();
                    keyEvent.consume();
                }
                setScrollHeight();
                snapScrollBar();

            } else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                KeyCode code = keyEvent.getCode();
                if (code == KeyCode.LEFT) {
                    text.moveDown();
                    rerenderAndUpdateBoundingBox();
                } else if (code == KeyCode.RIGHT) {
                    text.moveUp();
                    rerenderAndUpdateBoundingBox();
                } else if (code == KeyCode.UP) {
                    text.upArrow();
                    rerenderAndUpdateBoundingBox();
                } else if (code == KeyCode.DOWN) {
                    text.downArrow();
                    rerenderAndUpdateBoundingBox();
                }

                if (keyEvent.isShortcutDown()) {
                    if (keyEvent.getCode() == KeyCode.P) {
                        System.out.println((int) text.getCursorXPos() + ", " + (int) text.getCursorYPos());
                    }
                    if (keyEvent.getCode() == KeyCode.Z && text.undoSize() > 0) {
                        Action action = text.undo();
                        if (action.getAdd()){
                            root.getChildren().remove(action.getItem());
                        } else {
                            root.getChildren().add(action.getItem());  
                        }
                        rerenderAndUpdateBoundingBox();
                        keyEvent.consume();
                    }
                    if (keyEvent.getCode() == KeyCode.Y && text.redoSize() > 0) {
                        Action action = text.redo();
                        if (!action.getAdd()){
                            root.getChildren().remove(action.getItem());
                        } else {
                            root.getChildren().add(action.getItem()); 
                        }
                        rerenderAndUpdateBoundingBox();
                        
                    }

                    if (keyEvent.getCode() == KeyCode.EQUALS) {
                        text.changeFontSize(1);
                        text.rerender();
                        rerenderAndUpdateBoundingBox();
                    }

                    if (keyEvent.getCode() == KeyCode.MINUS) {
                        text.changeFontSize(-1);
                        text.rerender();
                        rerenderAndUpdateBoundingBox();                       
                    }

                    if (keyEvent.getCode() == KeyCode.S) {
                        save();                      
                    }

                    if (keyEvent.getCode() == KeyCode.W) {
                        text.printStream();                      
                    }

                }
                
                if (code == KeyCode.ENTER) {
                    text.addAtCurr("\n");
                    rerenderAndUpdateBoundingBox();
                    snapScrollBar();
                }
                setScrollHeight();
                snapScrollBar();
                keyEvent.consume();

            }
        }



    }

    public void snapScrollBar(){
        
        double amountAbove = text.cursorAboveScreen();
        double amountBelow = text.cursorBelowScreen();
        if (amountAbove != -1){
            scrollBar.setValue(windowHeight*(amountAbove/(text.getStructureHeight() - windowHeight)));
        } else if (amountBelow != -1){
            scrollBar.setValue(0);
            scrollBar.setValue(windowHeight*(amountBelow/(text.getStructureHeight() - windowHeight)));
        }
    }


    private class MouseClickEventHandler implements EventHandler<MouseEvent> {
        MouseClickEventHandler(Group root) {
        }


        @Override
        public void handle(MouseEvent mouseEvent) {
            double mousePressedX = mouseEvent.getX();
            double mousePressedY = mouseEvent.getY();
            text.mouseClickMove(mousePressedX, mousePressedY);
            rerenderAndUpdateBoundingBox();
        }

        
    }

    

    private class CursorEventHandler implements EventHandler<ActionEvent> {
        private int currentColorIndex = 0;
        private Color[] boxColors =
                {Color.WHITE, Color.BLACK};

        CursorEventHandler() {
            changeColor();
        }

        private void changeColor() {
            cursor.setFill(boxColors[currentColorIndex]);
            currentColorIndex = (currentColorIndex + 1) % boxColors.length;
        }

        @Override
        public void handle(ActionEvent event) {
            changeColor();
        }
    }

    public void makeCursorBlink() {
        final Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        CursorEventHandler cursorChange = new CursorEventHandler();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.5), cursorChange);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    public void setScrollHeight(){
        scrollBar.setMax(windowHeight);
    }

    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        windowWidth = 500;
        windowHeight = 500;
        Scene scene = new Scene(root, windowWidth, windowHeight, Color.WHITE);
        EventHandler<KeyEvent> keyEventHandler = new KeyEventHandler(root, windowWidth, windowHeight);

        scrollBar = new ScrollBar();
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.setPrefHeight(windowHeight);
        scrollBar.setMin(0);
        scrollBar.setMax(windowHeight);
        windowWidth = windowWidth - (int) scrollBar.getLayoutBounds().getWidth();
        text.setWindowWidth(windowWidth, true);

        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);
        scene.setOnMouseClicked(new MouseClickEventHandler(root));

        primaryStage.setTitle("Editor");

        root.getChildren().add(cursor);
        makeCursorBlink();

        
        root.getChildren().add(scrollBar);
        scrollBar.setLayoutX(windowWidth);
        text.setWindowWidth(windowWidth, false);

        scrollBar.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldValue,
                    Number newValue) {
                double value = newValue.doubleValue();
                if (text.getStructureHeight() > windowHeight){
                    double shift = ((value/windowHeight) * (text.getStructureHeight() - windowHeight)) + text.getShift();
                    text.rerender(-shift);
                    rerenderAndUpdateBoundingBox();
                }
            }
        });

        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenWidth,
                    Number newScreenWidth) {
                int newWindowWidth = newScreenWidth.intValue();
                
                windowWidth = newWindowWidth - (int) scrollBar.getLayoutBounds().getWidth();
                text.setWindowWidth(windowWidth, true);
                scrollBar.setLayoutX(windowWidth);
                rerenderAndUpdateBoundingBox();
            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenHeight,
                    Number newScreenHeight) {
                int newWindowHeight = newScreenHeight.intValue();
                windowHeight = newWindowHeight;
                text.setWindowHeight(newWindowHeight, true);
                scrollBar.setPrefHeight(newWindowHeight);
                rerenderAndUpdateBoundingBox();
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();
        rerenderAndUpdateBoundingBox();
    }

    private void rerenderAndUpdateBoundingBox() {
            double textHeight;
            if (text.isEmpty() || text.peekCurr() == text.getBuffer()){
                textHeight = text.getBuffer().getLayoutBounds().getHeight();
            } else {
                textHeight = text.getBuffer().getLayoutBounds().getHeight();
            }
            double x;
            if (text.getCursorXPos() > text.getWindowWidth()){
                cursor.setX(text.getWindowWidth() - text.getMargin());
            } else {
                cursor.setX(text.getCursorXPos());
            }

            cursor.setHeight(textHeight);
            cursor.setWidth(1);
            cursor.setY(text.getCursorYPos());

            setScrollHeight();
    }

    private void renderAll(){
        TextStorage.Node i = text.getFirst();
        while (i != text.getEnd()){
            root.getChildren().add(i.getItem());
            i = i.getNext();
        }
    }

    private void save(){      
        try {
            FileWriter writer = new FileWriter(file);
            TextStorage.Node i = text.getFirst();
            while (i != text.getEnd()){
                writer.write(i.getItem().getText());
                i = i.getNext();
            }
            writer.close();
        } catch (IOException ioException) {
            System.out.println("Error when copying; exception was: " + ioException);
        }
    }

    public static void main(String[] args) {
        
        launch(args);
    }

    public void print(String str){
        if (this.debug){
            System.out.println(str);
        }
    }


}

