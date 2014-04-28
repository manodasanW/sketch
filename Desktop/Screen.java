
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Screen extends JComponent implements ViewInterface {

    private Model model;
    private JFileChooser chooser;
    private JToolBar toolbar;
    private JSlider slider;
    private JButton play;
    private JToggleButton scale;
    private JToggleButton insert;
    private ActionListener frameUpdater;
    private Timer frameTimer;
    private boolean ctrlPressed;
    private int framesToAdd, savedMaximum;
    private Color color;

    public Screen() {
        model = new Model();
        model.addObserver(this);
        color = Color.black;
        toolbar = new JToolBar();

        chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON File", "json");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(filter);

        GridBagLayout layout = new GridBagLayout();
        JPanel sliderPanel = new JPanel(layout);
        play = new JButton(new ImageIcon("./icons/play.png"));
        play.addActionListener(new ActionListener() {
            boolean playSelected = true;
            Model.STATE previous;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (playSelected) {
                    previous = model.getState();
                    model.setState(Model.STATE.PLAY);
                    play.setIcon(new ImageIcon("./icons/pause.png"));
                    frameTimer.start();
                } else {
                    model.setState(previous);
                    frameTimer.stop();
                    play.setIcon(new ImageIcon("./icons/play.png"));
                }
                playSelected = !playSelected;
            }
        });
        sliderPanel.add(play);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 5, 6, 0);
        layout.addLayoutComponent(play, constraints);

        JButton stop = new JButton(new AbstractAction(null, new ImageIcon("./icons/stop.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getState() == Model.STATE.PLAY) {
                    play.doClick();
                }

                model.setFrame(0);
            }
        });
        sliderPanel.add(stop);

        constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 5, 6, 0);
        layout.addLayoutComponent(stop, constraints);

        slider = new JSlider(JSlider.HORIZONTAL, 0, 0, 0);
        slider.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (!ctrlPressed && e.isControlDown()) {
                    framesToAdd = 0;
                    savedMaximum = slider.getMaximum();
                    ctrlPressed = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (ctrlPressed && !e.isControlDown()) {
                    if (framesToAdd != 0) {
                        model.setFrame(model.getFrame() + 1);
                        model.insertFrames(framesToAdd);
                        slider.setMaximum(savedMaximum + framesToAdd);
                    }
                    framesToAdd = 0;
                    ctrlPressed = false;
                }
            }
        });
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {

                if (model.getSelection() != null) {
                    model.removeSelection();
                }

                JSlider source = (JSlider) e.getSource();
                if (ctrlPressed && model.getState() != Model.STATE.PLAY) {
                    framesToAdd += 2;
                    if (slider.getValue() == slider.getMaximum()) {
                        slider.setMaximum(slider.getMaximum() + 1);
                    }
                } else {
                    int val = (int) source.getValue();
                    model.setFrame(val);
                }
            }
        });
        sliderPanel.add(slider);

        constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 20, 0, 0);
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        layout.addLayoutComponent(slider, constraints);

        JButton newScreen = new JButton(new AbstractAction(null, new ImageIcon("./icons/new.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getState() == Model.STATE.PLAY) {
                    play.doClick();
                }

                model = new Model();
                model.addObserver(Screen.this);
            }
        });

        JButton draw = new JButton(new AbstractAction(null, new ImageIcon("./icons/draw.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getState() == Model.STATE.PLAY) {
                    play.doClick();
                }
                model.setState(Model.STATE.DRAW);
            }
        });

        JButton select = new JButton(new AbstractAction(null, new ImageIcon("./icons/select.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getState() == Model.STATE.PLAY) {
                    play.doClick();
                }
                model.setState(Model.STATE.SELECT);
            }
        });

        JButton erase = new JButton(new AbstractAction(null, new ImageIcon("./icons/erase.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getState() == Model.STATE.PLAY) {
                    play.doClick();
                }

                model.setState(Model.STATE.ERASE);
            }
        });

        insert = new JToggleButton(new AbstractAction(null, new ImageIcon("./icons/insert.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getState() == Model.STATE.PLAY) {
                    play.doClick();
                }

                model.setInsertMode(((JToggleButton) e.getSource()).isSelected());
            }
        });

        scale = new JToggleButton(new AbstractAction(null, new ImageIcon("./icons/scale.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setScaleTransform(((JToggleButton) e.getSource()).isSelected());
            }
        });

        JButton colorBtn = new JButton(new AbstractAction(null, new ImageIcon("./icons/color.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Color selected = JColorChooser.showDialog(Screen.this, "Choose Color", color);
                if (selected != null) {
                    color = selected;
                }
            }
        });

        JButton load = new JButton(new AbstractAction(null, new ImageIcon("./icons/load.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getState() == Model.STATE.PLAY) {
                    play.doClick();
                }

                int value = chooser.showOpenDialog(Screen.this);

                if (value == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();

                    if (!file.exists() && !file.getName().toLowerCase().endsWith(".json")) {
                        file = new File(chooser.getSelectedFile() + ".json");
                    }

                    if (!file.exists()) {
                        JOptionPane.showMessageDialog(Screen.this, "You must select a file which exists!");
                    } else {
                        boolean result = model.load(file);

                        if (!result) {
                            JOptionPane.showMessageDialog(Screen.this, "An error occurred while processing the file!");
                        }
                    }
                }
            }
        });

        JButton save = new JButton(new AbstractAction(null, new ImageIcon("./icons/save.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getState() == Model.STATE.PLAY) {
                    play.doClick();
                }

                int value = chooser.showSaveDialog(Screen.this);

                if (value == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if (!file.getName().toLowerCase().endsWith(".json")) {
                        file = new File(chooser.getSelectedFile() + ".json");
                    }

                    boolean result = model.save(file, Screen.this.getWidth(), Screen.this.getHeight() - play.getHeight());
                    if (!result) {
                        JOptionPane.showMessageDialog(Screen.this, "An error occurred while processing the file!");
                    }
                }
            }
        });

        frameUpdater = new ActionListener() {
            Point point = null;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getState() == Model.STATE.SELECT) {
                    if (model.getInsertMode()) {
                        slider.setMaximum(slider.getMaximum() + 1);
                    } else {
                        slider.setMaximum(Math.max(slider.getMaximum(), model.getFrame() + 1));
                    }
                    model.setFrame(model.getFrame() + 1);

                    if (point == null || (e.getActionCommand() != null && e.getActionCommand().equals("Reset"))) {
                        point = MouseInfo.getPointerInfo().getLocation();

                        if (model.getInsertMode()) {
                            model.addTransformation(new Point(0, 0), new Point(0, 0));
                        }
                    } else {
                        Point newPoint = MouseInfo.getPointerInfo().getLocation();
                        model.addTransformation(point, newPoint);
                        point = newPoint;
                    }
                } else if (model.getState() == Model.STATE.PLAY) {
                    if (model.getFrame() < slider.getMaximum()) {
                        model.setFrame(model.getFrame() + 1);
                    } else {
                        play.doClick();
                    }
                }
            }
        };
        frameTimer = new Timer(25, frameUpdater);
        MouseInputListener listener = new MouseInputListener() {
            boolean selected = false, selectionDragged = false, collectData = false;
            Timer collectionTimer = new Timer(25, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    collectData = true;
                }
            });

            @Override
            public void mouseClicked(MouseEvent e) {
                if (model.getState() == Model.STATE.ERASE) {
                    model.erase(e.getPoint());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                Model.STATE state = model.getState();

                if (state == Model.STATE.DRAW) {
                    model.buildObject(e.getPoint(), color, true);
                } else if (state == Model.STATE.SELECT) {
                    if (selected && model.getSelection() == null) {
                        selected = false;
                    }
                    if (selected && model.getSelection().getPolygon().contains(e.getPoint())) {
                        selectionDragged = true;
                        selected = false;
                        frameUpdater.actionPerformed(new ActionEvent(frameTimer, ActionEvent.RESERVED_ID_MAX + 1, "Reset"));
                        frameTimer.start();
                        model.removeSelection();
                    } else {
                        selected = false;
                        model.buildSelection(e.getPoint(), true);
                    }
                }
                collectionTimer.start();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Model.STATE state = model.getState();

                if (state == Model.STATE.DRAW) {
                    model.buildObject(e.getPoint(), color, false);
                } else if (state == Model.STATE.SELECT) {
                    if (selectionDragged) {
                        selectionDragged = false;
                        frameTimer.stop();
                    } else {
                        model.buildSelection(e.getPoint(), false);
                        selected = model.selectObject();
                    }
                }

                collectionTimer.stop();
                collectData = false;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!collectData) {
                    return;
                }

                Model.STATE state = model.getState();
                if (state == Model.STATE.DRAW) {
                    model.buildObject(e.getPoint(), color, false);
                } else if (state == Model.STATE.SELECT) {
                    if (selectionDragged) {
                    } else {
                        model.buildSelection(e.getPoint(), false);
                    }
                }
                collectData = false;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }
        };

        toolbar.add(newScreen);
        toolbar.add(load);
        toolbar.add(save);
        toolbar.addSeparator();
        toolbar.add(draw);
        toolbar.add(select);
        toolbar.add(erase);
        toolbar.add(colorBtn);
        toolbar.addSeparator();
        toolbar.add(insert);
        toolbar.add(scale);

        this.setLayout(new BorderLayout());
        this.add(toolbar, BorderLayout.NORTH);
        this.add(sliderPanel, BorderLayout.SOUTH);
        this.addMouseListener(listener);
        this.addMouseMotionListener(listener);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        ArrayList<GraphicData> polygons = model.getObjects();
        for (int a = 0; a < polygons.size(); a++) {
            Polygon polygon = polygons.get(a).getPolygon();
            g2.setColor(polygons.get(a).getColor());
            g2.drawPolyline(polygon.xpoints, polygon.ypoints, polygon.npoints);
        }

        GraphicData selection = model.getSelection();
        if (selection != null) {
            Polygon selectionP = selection.getPolygon();
            g2.setColor(selection.getColor());
            g2.drawPolyline(selectionP.xpoints, selectionP.ypoints, selectionP.npoints);
        }

        slider.setMaximum(model.getMaximumFrame());
        slider.setValue(model.getFrame());
        insert.setSelected(model.getInsertMode());
        scale.setSelected(model.getScaleTransform());
    }

    public static void main(String[] args) {
        Screen canvas = new Screen();
        JFrame f = new JFrame("Sketch");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(940, 480);
        f.setMinimumSize(new Dimension(320, 200));
        f.setContentPane(canvas);
        f.setVisible(true);
    }

    @Override
    public void updateView() {
        repaint();
    }
}
