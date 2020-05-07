import java.io.IOException;
import java.io.*;
import java.util.*;
import java.util.Random;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import javax.microedition.midlet.*;

public class RobboMIDlet
extends MIDlet
implements CommandListener {

 private Display dgDisplay;
 private RobboCanvas hdCanvas;


 static final Command ExitCommand = new Command("Exit", Command.EXIT, 0);

 public RobboMIDlet() {
  dgDisplay = Display.getDisplay(this);
 }

 protected void startApp() //throws MIDletStateChangeException
 {
  try {
   hdCanvas = new RobboCanvas(this, "/robbo_g1.png", "/strzal_d1.png", "/kapsula1.png");
   hdCanvas.start();
   hdCanvas.addCommand(ExitCommand);
   hdCanvas.setCommandListener(this);
  } catch (IOException ioe) {
   System.err.println("Problem loading image " + ioe);
  }

  dgDisplay.setCurrent(hdCanvas);
 }

 public void pauseApp() {}

 public void destroyApp(boolean unconditional) {
  hdCanvas.stop();
 }
 public void commandAction(Command c, Displayable s) {
  if (c.getCommandType() == Command.EXIT) {
   destroyApp(true);
   notifyDestroyed();
  }
 }
}

class RobboCanvas
extends GameCanvas
implements Runnable {

 String wersja = "V 0.61  11-03-2011";
 long czas1 = 0;
 long czas2 = 0;
 long czas3 = 0;
 int frameskip = 0; //przy 20fps 0, 10fps - 1, 5fps - 2, 2,5fps - 3

 int gr;
 int timeStep = 50;
 int timeStep2 = 50;

 int kierunek = 0; //0 - nic    1-gora 2-prawo 3-dol 4-lewo
 int kierunek_buf = 0; //0 - nic    1-gora 2-prawo 3-dol 4-lewo
 int fire = 0; //>0 wcisniety
 int fire_buf = 0; //>0 wcisniety

 int klatka = 0;
 int klatka_sek = 0;

 int bufor = 0; // 1-6 robot jest w buforze teleportacji od 1 do 6; 0 - bufor pusty
 int bufor_kier = 0; // kierunek robota wchodzacego do teleportu
 int bufor_x = 0; // wspolrzedne teleportu wejsciowego - ustawiane "niemozliwe" liczby po "okrazeniu" planszy
 int bufor_y = 0; // (zapobiega teleportacji do tego samego teleportu jesli istnieja inne)

 int i, j;
 int k = 0;
 int l = 0;
 int l2 = 0;
 int m = 0;

 int naboje = 0;
 int klucze = 0;
 int srubki = 0;
 int zycia = 0;
 int kapsula = 0;
 int kapsula_o = 0;
 int koniec_poziomu = 0;
 int lvl; //level startowy
 int max_lvl = 7;
 int restart = 0;
 int pos_y = 0;
 int posdoc_y = 0;

 int buforuj_fire = 0;
 int bufor_f = 0;

 int klocki = 16;
 int wysokosc = 10;
 int czestotliwosc = 12;
 int kamera = 0; //umieszczenie kamery w osi x (co do pixela, mnozyc przez zm. klocki)

 int robot_y = 0; //aktualna pozycja robota (y)
 int robot_x = 0;

 int[] wolne = new int[5]; //do badania wolnej drogi teleportacji docelowej


 int[] poziom_shadow = new int[1120];
 int[] poziom_shadow2 = new int[1120];

 int[] poziom_temp = new int[1120];
 int[] poziom_temp2 = new int[1120];


 int[] tab_klatki = new int[8];
 int nr_klatki = 0;


 int msx = 0;
 int sfx = 1;




 private RobboMIDlet midlet;

 private Player player;

 private Player s00;


 private Player s01;
 private Player s02;
 private Player s03;
 private Player s04;
 private Player s05;
 private Player s06;
 private Player s07;
 private Player s08;
 private Player s09;
 private Player s10;
 private Player s11;
 private Player s12;
 private Player s13;
 private Player s14;

 private LayerManager layerManager;


 private boolean gameRunning;
 private boolean Gra;
 private boolean collision = false;

 private int width;
 private int height;

 private long gameDuration;

 private Image tilesy;
 private TiledLayer mapa_blokow;
 private Image glowna;

 private Image font;


 public int keyStates = 0;
 public int keyStates3 = 0;
 public int keyStates4 = 0;
 public void keyReleased(int keyCode) {
  switch (getGameAction(keyCode)) {
   case Canvas.UP:
    keyStates &= -UP_PRESSED - 1;
    break;
   case Canvas.DOWN:
    keyStates &= -DOWN_PRESSED - 1;
    break;
   case Canvas.LEFT:
    keyStates &= -LEFT_PRESSED - 1;
    break;
   case Canvas.RIGHT:
    keyStates &= -RIGHT_PRESSED - 1;
    break;
   case Canvas.FIRE:
    keyStates &= -FIRE_PRESSED - 1;
    break;
   case Canvas.KEY_STAR:
    keyStates3 &= -FIRE_PRESSED - 1;
    break;
   case Canvas.KEY_NUM7:
    keyStates4 &= -FIRE_PRESSED - 1;
    break;
  }
 }

 protected void keyPressed(int keyCode) {
  switch (getGameAction(keyCode)) {
   case Canvas.UP:
    keyStates |= UP_PRESSED;
    break;
   case Canvas.DOWN:
    keyStates |= DOWN_PRESSED;
    break;
   case Canvas.LEFT:
    keyStates |= LEFT_PRESSED;
    break;
   case Canvas.RIGHT:
    keyStates |= RIGHT_PRESSED;
    break;
   case Canvas.FIRE:
    keyStates |= FIRE_PRESSED;
    break;
   case Canvas.KEY_STAR:
    keyStates3 |= FIRE_PRESSED;
    break;
   case Canvas.KEY_NUM7:
    keyStates4 |= FIRE_PRESSED;
    break;

  }

 }


 public RobboCanvas(RobboMIDlet hdmidlet, String carImageName, String obsImageName, String tloImageName)
 throws IOException {
  super(true);
  setFullScreenMode(true);

  this.midlet = hdmidlet; //used later

  layerManager = new LayerManager();

  width = getWidth();
  height = getHeight();

  layerManager.setViewWindow(0, 0, width, height);

 }


 public void start() {
  gameRunning = true;
  Thread gameThread = new Thread(this);
  gameThread.start();
 }

 public void stop() {
  gameRunning = false;
 }

 public void run() {
  while (1 == 1) {
   max_lvl = il_pozi("lvl.txt");
   width = getWidth();
   height = getHeight();


   lvl = 1;

   klocki = 16;

   if (width < 232)
    klocki = 12;
   if (width < 174) //174
    klocki = 8;

   wysokosc = (height / klocki) - 2;
   try {
    glowna = Image.createImage("/glowna2.png");
   } catch (IOException ioe) {
    System.err.println("Problem loading image " + ioe);
   }

   try {
    s00.stop();
   } catch (Exception ex) {}

   try {
    InputStream in = getClass().getResourceAsStream("/sound/01.wav");
    s01 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/02.wav");
    s02 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/03.wav");
    s03 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/04.wav");
    s04 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/05.wav");
    s05 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/06.wav");
    s06 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/07.wav");
    s07 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/08.wav");
    s08 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/09.wav");
    s09 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/10.wav");
    s10 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/11.wav");
    s11 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/12.wav");
    s12 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/13.wav");
    s13 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/sound/14.wav");
    s14 = Manager.createPlayer( in , "audio/x-wav"); in = getClass().getResourceAsStream("/music.mp3");
    s00 = Manager.createPlayer( in , "audio/mp3");
    s01.realize();
    s02.realize();
    s03.realize();
    s04.realize();
    s05.realize();
    s06.realize();
    s07.realize();
    s08.realize();
    s09.realize();
    s10.realize();
    s11.realize();
    s12.realize();
    s13.realize();
    s14.realize();
    s00.realize();
    VolumeControl vc;
    vc = (VolumeControl) s00.getControl("VolumeControl");
    vc.setLevel(100);
    s00.realize();
    s00.start();
   } catch (Exception ex) {}

   long musicstart = System.currentTimeMillis();

   Graphics g = getGraphics();
   g.setColor(0, 0, 0);
   g.fillRect(0, 0, getWidth(), getHeight());
   flushGraphics();


   int keyStates2 = getKeyStates();
   int pozycja = 0;



   while ((((keyStates & FIRE_PRESSED) == 0) && ((keyStates2 & FIRE_PRESSED) == 0)) || (pozycja > 1)) {
    keyStates2 = getKeyStates();
    if (((keyStates & UP_PRESSED) != 0) || ((keyStates2 & UP_PRESSED) != 0)) {
     pozycja--;
    }
    if (((keyStates & DOWN_PRESSED) != 0) || ((keyStates2 & DOWN_PRESSED) != 0)) {
     pozycja++;
    }
    if (pozycja < 0) {
     pozycja = 0;
    }
    if (pozycja > 5) {
     pozycja = 5;
    }
    if (pozycja == 2) {
     if (((keyStates & LEFT_PRESSED) != 0) || ((keyStates2 & LEFT_PRESSED) != 0)) {
      lvl--;
     }
     if (((keyStates & RIGHT_PRESSED) != 0) || ((keyStates2 & RIGHT_PRESSED) != 0)) {
      lvl++;
     }
     if (lvl < 1)
      lvl = 1;
     if (lvl > max_lvl)
      lvl = max_lvl;
    }
    if (pozycja == 3) {
     if (((keyStates & LEFT_PRESSED) != 0) || ((keyStates2 & LEFT_PRESSED) != 0)) {
      buforuj_fire = 0;
     }
     if (((keyStates & RIGHT_PRESSED) != 0) || ((keyStates2 & RIGHT_PRESSED) != 0)) {
      buforuj_fire = 1;
     }
    }
    if (pozycja == 4) {
     if (((keyStates & LEFT_PRESSED) != 0) || ((keyStates2 & LEFT_PRESSED) != 0)) {
      sfx = 0;
     }
     if (((keyStates & RIGHT_PRESSED) != 0) || ((keyStates2 & RIGHT_PRESSED) != 0)) {
      sfx = 1;
     }
    }
    if (pozycja == 5) {
     if (((keyStates & LEFT_PRESSED) != 0) || ((keyStates2 & LEFT_PRESSED) != 0)) {
      msx = 0;
     }
     if (((keyStates & RIGHT_PRESSED) != 0) || ((keyStates2 & RIGHT_PRESSED) != 0)) {
      msx = 1;
     }
    }
    g.setColor(0, 0, 0);
    g.fillRect(0, 0, getWidth(), getHeight());
    g.drawImage(glowna, (width / 2) - 88, 10, Graphics.TOP | Graphics.LEFT);

    g.setColor(255, 255, 255);
    g.drawString("Program Oryginalny:", (width / 2) - 50, 45, Graphics.TOP | Graphics.LEFT);
    g.drawString("Janusz Pelc", (width / 2) - 30, 60, Graphics.TOP | Graphics.LEFT);
    g.drawString("Kod J2ME:", (width / 2) - 25, 75, Graphics.TOP | Graphics.LEFT);
    g.drawString("Grzegorz Korycki", (width / 2) - 45, 90, Graphics.TOP | Graphics.LEFT);

    if (pozycja == 0) {
     g.setColor(63, 191, 255);
     g.drawString(">  FULL-SCREEN  <", (width / 2) - 40, height - 90, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  16x10 (ATARI)  ", (width / 2) - 40, height - 75, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("    POZIOM:" + lvl + "     ", (width / 2) - 40, height - 60, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  BUFOR FIRE: " + buforuj_fire + "  ", (width / 2) - 40, height - 45, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  DZWIEK: " + sfx + "  ", (width / 2) - 40, height - 30, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  MUZYKA: " + msx + "  ", (width / 2) - 40, height - 15, Graphics.TOP | Graphics.LEFT);
    }
    if (pozycja == 1) {
     g.setColor(0, 63, 255);
     g.drawString("   FULL-SCREEN   ", (width / 2) - 40, height - 90, Graphics.TOP | Graphics.LEFT);
     g.setColor(63, 191, 255);
     g.drawString("> 16x10 (ATARI) <", (width / 2) - 40, height - 75, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("    POZIOM:" + lvl + "     ", (width / 2) - 40, height - 60, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  BUFOR FIRE: " + buforuj_fire + "  ", (width / 2) - 40, height - 45, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  DZWIEK: " + sfx + "  ", (width / 2) - 40, height - 30, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  MUZYKA: " + msx + "  ", (width / 2) - 40, height - 15, Graphics.TOP | Graphics.LEFT);
    }
    if (pozycja == 2) {
     g.setColor(0, 63, 255);
     g.drawString("   FULL-SCREEN   ", (width / 2) - 40, height - 90, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  16x10 (ATARI)  ", (width / 2) - 40, height - 75, Graphics.TOP | Graphics.LEFT);
     g.setColor(63, 191, 255);
     g.drawString(">   POZIOM:" + lvl + "    <", (width / 2) - 40, height - 60, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  BUFOR FIRE: " + buforuj_fire + "  ", (width / 2) - 40, height - 45, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  DZWIEK: " + sfx + "  ", (width / 2) - 40, height - 30, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  MUZYKA: " + msx + "  ", (width / 2) - 40, height - 15, Graphics.TOP | Graphics.LEFT);
    }
    if (pozycja == 3) {
     g.setColor(0, 63, 255);
     g.drawString("   FULL-SCREEN   ", (width / 2) - 40, height - 90, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  16x10 (ATARI)  ", (width / 2) - 40, height - 75, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("    POZIOM:" + lvl + "     ", (width / 2) - 40, height - 60, Graphics.TOP | Graphics.LEFT);
     g.setColor(63, 191, 255);
     g.drawString("> BUFOR FIRE: " + buforuj_fire + " <", (width / 2) - 40, height - 45, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  DZWIEK: " + sfx + "  ", (width / 2) - 40, height - 30, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  MUZYKA: " + msx + "  ", (width / 2) - 40, height - 15, Graphics.TOP | Graphics.LEFT);
    }
    if (pozycja == 4) {
     g.setColor(0, 63, 255);
     g.drawString("   FULL-SCREEN   ", (width / 2) - 40, height - 90, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  16x10 (ATARI)  ", (width / 2) - 40, height - 75, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("    POZIOM:" + lvl + "     ", (width / 2) - 40, height - 60, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  BUFOR FIRE: " + buforuj_fire + " <", (width / 2) - 40, height - 45, Graphics.TOP | Graphics.LEFT);
     g.setColor(63, 191, 255);
     g.drawString("> DZWIEK: " + sfx + " <", (width / 2) - 40, height - 30, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  MUZYKA: " + msx + "  ", (width / 2) - 40, height - 15, Graphics.TOP | Graphics.LEFT);
    }
    if (pozycja == 5) {
     g.setColor(0, 63, 255);
     g.drawString("   FULL-SCREEN   ", (width / 2) - 40, height - 90, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  16x10 (ATARI)  ", (width / 2) - 40, height - 75, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("    POZIOM:" + lvl + "     ", (width / 2) - 40, height - 60, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  BUFOR FIRE: " + buforuj_fire + " <", (width / 2) - 40, height - 45, Graphics.TOP | Graphics.LEFT);
     g.setColor(0, 63, 255);
     g.drawString("  DZWIEK: " + sfx + "  ", (width / 2) - 40, height - 30, Graphics.TOP | Graphics.LEFT);
     g.setColor(63, 191, 255);
     g.drawString("> MUZYKA: " + msx + " <", (width / 2) - 40, height - 15, Graphics.TOP | Graphics.LEFT);
    }
    g.setColor(255, 63, 0);
    g.drawString(wersja, 0, 0, Graphics.TOP | Graphics.LEFT);

    flushGraphics();

    try {
     Thread.sleep(100); //- duration);
    } catch (InterruptedException ie) {
     stop();
    }
   }

   if (pozycja == 1) {
    wysokosc = 10;
   }



   if (klocki == 8) {
    try {
     tilesy = Image.createImage("/tiles/8.png");

    } catch (IOException ioe) {
     System.err.println("Problem loading image " + ioe);
    }
   }


   if (klocki == 12) {
    try {
     tilesy = Image.createImage("/tiles/12.png");

    } catch (IOException ioe) {
     System.err.println("Problem loading image " + ioe);
    }
   }
   if (klocki == 16) {
    try {
     tilesy = Image.createImage("/tiles/16.png");

    } catch (IOException ioe) {
     System.err.println("Problem loading image " + ioe);
    }
   }

   if (klocki == 16)
    mapa_blokow = new TiledLayer(16, wysokosc + 1, tilesy, 16, 16);
   if (klocki == 12)
    mapa_blokow = new TiledLayer(16, wysokosc + 1, tilesy, 12, 12);
   if (klocki == 8)
    mapa_blokow = new TiledLayer(16, wysokosc + 1, tilesy, 8, 8);


   Gra = true;
   zycia = 9;
   while (Gra) {

    //start poziomu
    if (lvl < 10) {
     zaladujpoziom("/levels/level00" + lvl + ".txt");
    }

    if ((lvl >= 10) && (lvl < 100)) {
     zaladujpoziom("/levels/level0" + lvl + ".txt");
    }

    if (lvl >= 100) {
     zaladujpoziom("/levels/level" + lvl + ".txt");
    }
    srubki = 0;

    for (i = 0; i < (16 * 70); i++) {
     if ((poziom_shadow[i]) == 8) {
      srubki++;
     }
    }


    long startTime = System.currentTimeMillis();
    stworzshadow2_start();

    try {
     font = Image.createImage("/font12.png");
    } catch (IOException ioe) {
     System.err.println("Problem loading fonts " + ioe);
    }



    bufor_f = 0;
    restart = 0;
    naboje = 0;
    klucze = 0;
    kapsula = 0;
    kapsula_o = 0;
    koniec_poziomu = 0;
    gameRunning = true;

    if (msx == 0) {
     try {
      s00.stop();
     } catch (Exception ex) {}
    }

    while (gameRunning) //is true
    {
     if (msx == 1) {
      if ((System.currentTimeMillis() - musicstart) > 223500) {
       musicstart = System.currentTimeMillis();
       try {
        s00.stop();
       } catch (Exception ex) {}
       try {
        s00.start();
       } catch (Exception ex) {}
      }
     }

     tick();
     if ((kierunek_buf != 0) && ((klatka_sek != 0) || (klatka_sek != 4) || (klatka_sek != 8) || (klatka_sek != 12))) {
      kierunek = 0;
     }
     input();
     if ((klatka_sek == 0) || (klatka_sek == 4) || (klatka_sek == 8) || (klatka_sek == 12)) {
      kierunek_buf = kierunek;
      stworzshadow2();
      poruszelementy();
      poruszpostacie();
     }

     if (koniec_poziomu > 0) {
      koniec_poziomu++;
      if (koniec_poziomu > 16) {
       koniec_poziomu = 16;
      }
     }
     if (posdoc_y > pos_y) {
      pos_y++;
      pos_y++;
      pos_y++;
     }
     if (posdoc_y < pos_y) {
      pos_y--;
      pos_y--;
      pos_y--;
     }
     if (pos_y < 0)
      pos_y = 0;
     if (pos_y > 8 * (70 - wysokosc - 1))
      pos_y = 8 * (70 - wysokosc - 1);


     long endTime = System.currentTimeMillis();
     long duration = (int)(endTime - startTime);
     gameDuration = duration / 1000; //game time in seconds

     czas3 = System.currentTimeMillis();
     klatka++;
     if ((((int)(czas3 - czas2)) + l2 > timeStep2) && (frameskip < 2)) {
      frameskip++;
      timeStep2 = timeStep2 + 40;
     }


     if (klatka > frameskip)
      klatka = 0;
     klatka_sek++;
     if (klatka_sek >= 15)
      klatka_sek = 0;

     if (restart > 0) {
      restart--;
      if (restart == 0) {
       gameRunning = false;
       zycia--;
       if (zycia < 0) {
        zycia = 0;
        Gra = false;
       }
      }
     }


     czas3 = System.currentTimeMillis();
     if (klatka == 0) {
      gr = 1;
      render(g);
      czas1 = System.currentTimeMillis();
      if (czas2 != 0) {
       l2 = 0;
       tab_klatki[nr_klatki] = (int)(czas1 - czas3);
       for (i = 0; i < 8; i++) {
        if (tab_klatki[i] > l2)
         l2 = tab_klatki[i]; //czas maksymalnego renderu w ciagu 8 ostatnich klatek
       }
       nr_klatki++;
       if (nr_klatki > 7)
        nr_klatki = 0;

       czas3 = System.currentTimeMillis();
       l = (int)(czas3 - czas2);
       if (l == 0)
        l = 16;
       try {
        timeStep = (timeStep2 - l);
        if (timeStep < 5)
         timeStep = 5;

        Thread.sleep(timeStep); //- duration);
       } catch (InterruptedException ie) {
        stop();
       }

       if ((l) < 200) {
        frameskip = 3;
        timeStep2 = 200;
       }
       if ((l) < 150) {
        frameskip = 2;
        timeStep2 = 150;
       }
       if ((l) < 100) {
        frameskip = 1;
        timeStep2 = 100;
       }
       if (l < 50) {
        frameskip = 0;
        timeStep2 = 50;
       }
       if ((l) >= 350) {
        frameskip = 7;
        if (l > 390) {
         timeStep2 = 10;
        } else {
         timeStep2 = 400;
        }
       }
       frameskip = 0;
       timeStep2 = 40;
       czas1 = System.currentTimeMillis();
      }
      czas2 = czas1;
     } //koniec if(klatka==0)
     else {
      try {
       i = 2;
       Thread.sleep(i); //- duration);
      } catch (InterruptedException ie) {
       stop();
      }
     }
     if ((srubki == 0) && (kapsula == 0)) {
      kapsula = 1;
      kapsula_o = 1;
      if (sfx == 1) {
       try {
        s05.realize();
        s05.start();
       } catch (Exception ex) {}
      }

     }

    }
    if (lvl > max_lvl)
     Gra = false;
   }
  }
 }


 private void tick() {
  if (!collision)
   checkCollision();
  if (collision) {

  }
 }

 private void input() {
  int keyStates2 = getKeyStates();


  k = 0;

  if (((keyStates & LEFT_PRESSED) != 0) || ((keyStates2 & LEFT_PRESSED) != 0)) {
   k = 1;
   kierunek = 4;
  }
  if (((keyStates & RIGHT_PRESSED) != 0) || ((keyStates2 & RIGHT_PRESSED) != 0)) {
   k = 1;
   kierunek = 2;
  }
  if (((keyStates & UP_PRESSED) != 0) || ((keyStates2 & UP_PRESSED) != 0)) {
   k = 1;
   kierunek = 1;
  }
  if (((keyStates & DOWN_PRESSED) != 0) || ((keyStates2 & DOWN_PRESSED) != 0)) {
   k = 1;
   kierunek = 3;
  }
  if (k == 0) {
   fire_buf = 0;
   if (bufor_f == -1) {
    bufor_f = 0;
   }
  }

  if (((keyStates & FIRE_PRESSED) != 0) || ((keyStates2 & FIRE_PRESSED) != 0)) {
   if (buforuj_fire == 1) {
    if (naboje > 0)
     bufor_f = 15;

    fire = 1;
   } else {
    fire = 1;
   }

  } else {
   fire_buf = 0;
  }
  if (bufor_f > 0) {
   fire = 1;
   bufor_f--;
  }
  if (bufor_f == -1) //zapobiega biegnieciu za nabojem przy uzyciu funkcji BUFOR FIRE z menu
   kierunek = 0;
 }
 private void render(Graphics g) {
  if (gr == 1) {
   if (kapsula_o == 1) {
    g.setColor(255, 255, 255); // white
    kapsula_o = 0;
   } else {
    g.setColor(0, 0, 0); // white
   }
   g.fillRect(0, 0, getWidth(), getHeight());

   for (i = 0; i < 16; i++) {
    for (j = 0; j < wysokosc + 1; j++) {
     m = pos_y / 8;
     switch (poziom_shadow[i + ((j + m) * 16)]) {




      case 0:
       mapa_blokow.setCell(i, j, 31);
       break;
      case 1:
       mapa_blokow.setCell(i, j, 30);
       break;
      case 2:
       mapa_blokow.setCell(i, j, 40);
       break;
      case 3:
       mapa_blokow.setCell(i, j, 41);
       break;
      case 4:
       mapa_blokow.setCell(i, j, 42);
       break;
      case 5:
       mapa_blokow.setCell(i, j, 12);
       break;
      case 6:
       mapa_blokow.setCell(i, j, 13);
       break;
      case 7:
       mapa_blokow.setCell(i, j, 14);
       break;
      case 8:
       mapa_blokow.setCell(i, j, 43);
       break;
      case 9:
       mapa_blokow.setCell(i, j, 17);
       break;
      case 10:
       mapa_blokow.setCell(i, j, 18);
       break;

      case 11:
       mapa_blokow.setCell(i, j, 48);
       break;
      case 12:
       mapa_blokow.setCell(i, j, 49);
       break;
      case 13:
       mapa_blokow.setCell(i, j, 48);
       break;
      case 14:
       mapa_blokow.setCell(i, j, 49);
       break;
      case 15:
       mapa_blokow.setCell(i, j, 48);
       break;
      case 16:
       mapa_blokow.setCell(i, j, 49);
       break;
      case 17:
       mapa_blokow.setCell(i, j, 48);
       break;
      case 18:
       mapa_blokow.setCell(i, j, 49);
       break;
      case 19:
       mapa_blokow.setCell(i, j, 48);
       break;
      case 20:
       mapa_blokow.setCell(i, j, 49);
       break;

      case 21:
       mapa_blokow.setCell(i, j, 48);
       break;
      case 22:
       mapa_blokow.setCell(i, j, 49);
       break;
      case 23:
       mapa_blokow.setCell(i, j, 2);
       break;
      case 24:
       mapa_blokow.setCell(i, j, 15);
       break;
      case 25:
       mapa_blokow.setCell(i, j, 16);
       break;
      case 26:
       mapa_blokow.setCell(i, j, 8);
       break;
      case 27:
       mapa_blokow.setCell(i, j, 10);
       break;
      case 28:
       mapa_blokow.setCell(i, j, 7);
       break;
      case 29:
       mapa_blokow.setCell(i, j, 9);
       break;
      case 30:
       mapa_blokow.setCell(i, j, 8);
       break;

      case 31:
       mapa_blokow.setCell(i, j, 10);
       break;
      case 32:
       mapa_blokow.setCell(i, j, 7);
       break;
      case 33:
       mapa_blokow.setCell(i, j, 9);
       break;
      case 34:
       mapa_blokow.setCell(i, j, 8);
       break;
      case 35:
       mapa_blokow.setCell(i, j, 10);
       break;
      case 36:
       mapa_blokow.setCell(i, j, 7);
       break;
      case 37:
       mapa_blokow.setCell(i, j, 9);
       break;
      case 38:
       mapa_blokow.setCell(i, j, 8);
       break;
      case 39:
       mapa_blokow.setCell(i, j, 10);
       break;
      case 40:
       mapa_blokow.setCell(i, j, 7);
       break;

      case 41:
       mapa_blokow.setCell(i, j, 9);
       break;
      case 42:
       mapa_blokow.setCell(i, j, 11);
       break;
      case 43:
       mapa_blokow.setCell(i, j, 11);
       break;
      case 44:
       mapa_blokow.setCell(i, j, 51);
       break;
      case 45:
       mapa_blokow.setCell(i, j, 52);
       break;
      case 46:
       mapa_blokow.setCell(i, j, 1);
       break;
      case 47:
       mapa_blokow.setCell(i, j, 12);
       break;
      case 48:
       mapa_blokow.setCell(i, j, 1);
       break;
      case 51:
       mapa_blokow.setCell(i, j, 44);
       break;
      case 52:
       mapa_blokow.setCell(i, j, 46);
       break;
      case 53:
       mapa_blokow.setCell(i, j, 44);
       break;
      case 54:
       mapa_blokow.setCell(i, j, 44);
       break;
      case 55:
       mapa_blokow.setCell(i, j, 46);
       break;
      case 56:
       mapa_blokow.setCell(i, j, 46);
       break;
      case 57:
       mapa_blokow.setCell(i, j, 50);
       break;
      case 58:
       mapa_blokow.setCell(i, j, 19);
       break;
      case 59:
       mapa_blokow.setCell(i, j, 34); //34 - 35
       break;
      case 60:
       mapa_blokow.setCell(i, j, 35);
       break;

      case 61:
       mapa_blokow.setCell(i, j, 38);
       break;
      case 62:
       mapa_blokow.setCell(i, j, 39);
       break;
      case 63:
       mapa_blokow.setCell(i, j, 32);
       break;
      case 64:
       mapa_blokow.setCell(i, j, 33);
       break;
      case 65:
       mapa_blokow.setCell(i, j, 36);
       break;
      case 66:
       mapa_blokow.setCell(i, j, 37);
       break;
      case 67:
       mapa_blokow.setCell(i, j, 24);
       break;
      case 68:
       mapa_blokow.setCell(i, j, 25);
       break;
      case 69:
       mapa_blokow.setCell(i, j, 21);
       break;
      case 70:
       mapa_blokow.setCell(i, j, 23);
       break;

      case 71:
       mapa_blokow.setCell(i, j, 21);
       break;
      case 72:
       mapa_blokow.setCell(i, j, 23);
       break;

      case 75:
       mapa_blokow.setCell(i, j, 3);
       break;
      case 76:
       mapa_blokow.setCell(i, j, 4);
       break;
      case 77:
       mapa_blokow.setCell(i, j, 5);
       break;
      case 78:
       mapa_blokow.setCell(i, j, 6);
       break;
      case 79:
       mapa_blokow.setCell(i, j, 5);
       break;
      case 80:
       mapa_blokow.setCell(i, j, 4);
       break;

      case 81:
       mapa_blokow.setCell(i, j, 3);
       break;
      case 82:
       mapa_blokow.setCell(i, j, 3);
       break;
      case 83:
       mapa_blokow.setCell(i, j, 4);
       break;
      case 84:
       mapa_blokow.setCell(i, j, 5);
       break;
      case 85:
       mapa_blokow.setCell(i, j, 6);
       break;
      case 86:
       mapa_blokow.setCell(i, j, 5);
       break;
      case 87:
       mapa_blokow.setCell(i, j, 4);
       break;
      case 88:
       mapa_blokow.setCell(i, j, 3);
       break;
      case 89:
       mapa_blokow.setCell(i, j, 20);
       break;
      case 90:
       mapa_blokow.setCell(i, j, 22);
       break;

      case 91:
       mapa_blokow.setCell(i, j, 20);
       break;
      case 92:
       mapa_blokow.setCell(i, j, 22);
       break;
      case 93:
       mapa_blokow.setCell(i, j, 20);
       break;
      case 94:
       mapa_blokow.setCell(i, j, 22);
       break;
      case 95:
       mapa_blokow.setCell(i, j, 20);
       break;
      case 96:
       mapa_blokow.setCell(i, j, 22);
       break;
      case 97:
       mapa_blokow.setCell(i, j, 26);
       break;
      case 98:
       mapa_blokow.setCell(i, j, 27);
       break;
      case 99:
       mapa_blokow.setCell(i, j, 26);
       break;
      case 100:
       mapa_blokow.setCell(i, j, 27);
       break;

      case 101:
       mapa_blokow.setCell(i, j, 26);
       break;
      case 102:
       mapa_blokow.setCell(i, j, 27);
       break;
      case 103:
       mapa_blokow.setCell(i, j, 26);
       break;
      case 104:
       mapa_blokow.setCell(i, j, 27);
       break;
      case 105:
       mapa_blokow.setCell(i, j, 28);
       break;
      case 106:
       mapa_blokow.setCell(i, j, 29);
       break;
      case 107:
       mapa_blokow.setCell(i, j, 28);
       break;
      case 108:
       mapa_blokow.setCell(i, j, 29);
       break;
      case 109:
       mapa_blokow.setCell(i, j, 28);
       break;
      case 110:
       mapa_blokow.setCell(i, j, 29);
       break;

      case 111:
       mapa_blokow.setCell(i, j, 28);
       break;
      case 112:
       mapa_blokow.setCell(i, j, 29);
       break;

     }
    }
   }

   mapa_blokow.setPosition((width - (16 * klocki)) / 2, 0 - (pos_y - (m * 8)));


   if (koniec_poziomu > 0) {
    for (i = 0; i < koniec_poziomu; i++) {
     for (j = 0; j < wysokosc + 1; j++) {
      mapa_blokow.setCell(i, j, 30);
     }
    }
    if (koniec_poziomu >= 16) {
     lvl++;
     gameRunning = false;
    }

   }
   if ((restart <= 16) && (restart != 0)) {
    for (i = 0; i < (16 - restart); i++) {
     for (j = 0; j < wysokosc + 1; j++) {
      mapa_blokow.setCell(i, j, 30);
     }
    }
    if (koniec_poziomu >= 16) {
     lvl++;
     gameRunning = false;
    }

   }


   mapa_blokow.paint(g);


   if (buforuj_fire == 1) {
    if (bufor_f > 0) {
     g.setColor(255, 0, 0);
     g.fillRect(getWidth() - 8, getHeight() - 8, 8, 8);
    }
    if (bufor_f < 0) {
     g.setColor(0, 0, 255);
     g.fillRect(getWidth() - 8, getHeight() - 8, 8, 8);
    }
   }


   g.setColor(0, 0, 0);
   g.drawString("L: " + Integer.toString(lvl), 1, 2, Graphics.TOP | Graphics.LEFT);
   g.drawString("L: " + Integer.toString(lvl), 3, 2, Graphics.TOP | Graphics.LEFT);
   g.drawString("L: " + Integer.toString(lvl), 2, 1, Graphics.TOP | Graphics.LEFT);
   g.drawString("L: " + Integer.toString(lvl), 2, 3, Graphics.TOP | Graphics.LEFT);

   g.drawString("FP" + Integer.toString(20 / (frameskip + 1)), width - 51, 2, Graphics.TOP | Graphics.LEFT);
   g.drawString("FP" + Integer.toString(20 / (frameskip + 1)), width - 49, 2, Graphics.TOP | Graphics.LEFT);
   g.drawString("FP" + Integer.toString(20 / (frameskip + 1)), width - 50, 1, Graphics.TOP | Graphics.LEFT);
   g.drawString("FP" + Integer.toString(20 / (frameskip + 1)), width - 50, 3, Graphics.TOP | Graphics.LEFT);


   g.setColor(0, 63, 255);
   g.drawString("L: " + Integer.toString(lvl), 2, 2, Graphics.TOP | Graphics.LEFT);

   g.setColor(255, 63, 0);
   g.drawString("FP" + Integer.toString(20 / (frameskip + 1)), width - 50, 2, Graphics.TOP | Graphics.LEFT);

   g.setColor(255, 0, 255);
   g.setColor(255, 0, 0);
   g.setColor(255, 255, 255);

   g.drawString(wersja, 0, height - 14, Graphics.TOP | Graphics.LEFT);

   g.setColor(255, 0, 0);

   g.drawString("Z:" + Integer.toString(zycia) + " K:" + Integer.toString(klucze) + " N:" + Integer.toString(naboje) + " S:" + Integer.toString(srubki), (width / 2) - 44, height - 16, Graphics.TOP | Graphics.LEFT);

  } else {

  }

  layerManager.paint(g, 0, 0);

  flushGraphics();


 }

 private void checkCollision() {

 }


 private void stworzshadow2_start() {
  int kratka;
  for (i = 0; i < 16; i++) {
   for (j = 0; j < 70; j++) {
    kratka = i + (j * 16);
    if (poziom_shadow[kratka] == 0) {
     poziom_shadow2[kratka] = 0;
    } else {
     if (((poziom_shadow[kratka] >= 1) && (poziom_shadow[kratka] <= 10)) || (poziom_shadow[kratka] == 47)) {
      poziom_shadow2[kratka] = 6;
     }
     if ((poziom_shadow[kratka] >= 11) && (poziom_shadow[kratka] <= 43)) {
      poziom_shadow2[kratka] = 7;
     }
     if ((poziom_shadow[kratka] == 46) || (poziom_shadow[kratka] == 48)) { //bomba
      poziom_shadow2[kratka] = 15;
     }
     if (poziom_shadow[kratka] == 44) {
      poziom_shadow2[kratka] = 21;
     }
     if (((poziom_shadow[kratka] >= 57) && (poziom_shadow[kratka] <= 72)) || (poziom_shadow[kratka] == 45) || ((poziom_shadow[kratka] >= 89) && (poziom_shadow[kratka] <= 112))) {
      poziom_shadow2[kratka] = 20;
     }
    }
   }
  }
 }

 private void stworzshadow2() {
  int kratka;
  for (i = 0; i < 16; i++) {
   for (j = 0; j < 70; j++) {
    kratka = i + (j * 16);
    if (poziom_shadow[kratka] == 0) {
     poziom_shadow2[kratka] = 0;
    } else {
     if (((poziom_shadow[kratka] >= 1) && (poziom_shadow[kratka] <= 10)) || (poziom_shadow[kratka] == 47)) {
      poziom_shadow2[kratka] = 6;
     }
     if ((poziom_shadow[kratka] >= 11) && (poziom_shadow[kratka] <= 43) && (poziom_shadow[kratka] != 25)) {
      poziom_shadow2[kratka] = 7;
     }
     if ((poziom_shadow[kratka] == 46) || (poziom_shadow[kratka] == 48)) { //bomba
      poziom_shadow2[kratka] = 15;
     }
     if (poziom_shadow[kratka] == 44) {
      poziom_shadow2[kratka] = 21;
     }
     if (((poziom_shadow[kratka] >= 57) && (poziom_shadow[kratka] <= 72)) || (poziom_shadow[kratka] == 45) || ((poziom_shadow[kratka] >= 89) && (poziom_shadow[kratka] <= 112))) {
      poziom_shadow2[kratka] = 20;
     }
    }

   }
  }
 }


 private void poruszelementy() {
  int tymcz1;
  System.arraycopy(poziom_shadow2, 0, poziom_temp2, 0, 1120);
  System.arraycopy(poziom_shadow, 0, poziom_temp, 0, 1120);

  int kratka;
  for (j = 0; j < 70; j++) {
   for (i = 0; i < 16; i++) {
    kratka = i + (j * 16);
    if ((poziom_shadow[kratka] > 4)) {

     if (((poziom_shadow[kratka]) >= 69) && ((poziom_shadow[kratka]) <= 72)) { //nietoperz strzelajacy (strza�)
      if (losowa2(czestotliwosc, i, j) == 10) {
       if (poziom_temp2[(kratka + 16)] == 0) {
        poziom_temp[(kratka + 16)] = 55;
        poziom_temp2[(kratka + 16)] = 3;
       }
       if ((poziom_temp2[(kratka + 16)]) == 20) { //jesli
        poziom_temp2[(kratka + 16)] = 6;
        poziom_temp[(kratka + 16)] = 77;
       }
       if ((poziom_temp2[(kratka + 16)]) == 21) { //jesli
        poziom_temp2[(kratka + 16)] = 6;
        poziom_temp[(kratka + 16)] = 82;
       }
       if ((poziom_shadow2[(kratka + 16)]) == 15) { //bomba
        eksplozja(i, j + 1);
       }

      }
     }


     //oczka
     if (((poziom_shadow[kratka]) == 67) || ((poziom_shadow[kratka]) == 69) || ((poziom_shadow[kratka]) == 71) || ((poziom_shadow[kratka]) == 89) || ((poziom_shadow[kratka]) == 91) || ((poziom_shadow[kratka]) == 93) || ((poziom_shadow[kratka]) == 95) || ((poziom_shadow[kratka]) == 97) || ((poziom_shadow[kratka]) == 99) || ((poziom_shadow[kratka]) == 101) || ((poziom_shadow[kratka]) == 103) || ((poziom_shadow[kratka]) == 105) || ((poziom_shadow[kratka]) == 107) || ((poziom_shadow[kratka]) == 109) || ((poziom_shadow[kratka]) == 111)) { //potworki
      if ((klatka_sek == 0) || (klatka_sek == 8))
       poziom_temp[kratka]++;
     }
     if (((poziom_shadow[kratka]) == 68) || ((poziom_shadow[kratka]) == 70) || ((poziom_shadow[kratka]) == 72) || ((poziom_shadow[kratka]) == 90) || ((poziom_shadow[kratka]) == 92) || ((poziom_shadow[kratka]) == 94) || ((poziom_shadow[kratka]) == 96) || ((poziom_shadow[kratka]) == 98) || ((poziom_shadow[kratka]) == 100) || ((poziom_shadow[kratka]) == 102) || ((poziom_shadow[kratka]) == 104) || ((poziom_shadow[kratka]) == 106) || ((poziom_shadow[kratka]) == 108) || ((poziom_shadow[kratka]) == 110) || ((poziom_shadow[kratka]) == 112)) { //potworki
      if ((klatka_sek == 0) || (klatka_sek == 8))
       poziom_temp[kratka]--;
     }

     if ((poziom_shadow[kratka]) == 48) { //eksplodujaca bomba (zaplon)
      //dzwiek eksplozji
      eksplozja(i, j);
     }
     //kapsula
     if ((poziom_shadow[kratka]) == 47) { //kapsula
      if (kapsula == 1)
       poziom_temp[kratka] = 6;
     }
     if (((poziom_shadow[kratka]) == 5) || ((poziom_shadow[kratka]) == 6)) { //kapsula
      if (klatka_sek == 4) {
       poziom_temp[kratka]++;
       if (poziom_temp[kratka] > 6)
        poziom_temp[kratka] = 5;
      }
     }
     if (((poziom_shadow[kratka]) == 11) || ((poziom_shadow[kratka]) == 13) || ((poziom_shadow[kratka]) == 15) || ((poziom_shadow[kratka]) == 17) || ((poziom_shadow[kratka]) == 19) || ((poziom_shadow[kratka]) == 21)) { //dym jakikolwiek
      if (klatka_sek == 0)
       (poziom_temp[kratka]) ++;
     }
     if (((poziom_shadow[kratka]) == 12) || ((poziom_shadow[kratka]) == 14) || ((poziom_shadow[kratka]) == 16) || ((poziom_shadow[kratka]) == 18) || ((poziom_shadow[kratka]) == 20) || ((poziom_shadow[kratka]) == 22)) { //dym jakikolwiek
      if (klatka_sek == 8)
       (poziom_temp[kratka]) --;
     }

     if (((poziom_shadow[kratka]) >= 82) && ((poziom_shadow[kratka]) <= 88)) { //dym znak zapytania
      if (poziom_shadow[kratka] == 88) {
       switch (losowa2(10, i, j)) {
        case 1: //dym
         poziom_temp2[kratka] = 6;
         poziom_temp[kratka] = 75;
         break;
        case 2: //znak zapytania
         poziom_temp2[kratka] = 21;
         poziom_temp[kratka] = 44;
         break;
        case 3: //srubka
         poziom_temp2[kratka] = 6;
         poziom_temp[kratka] = 8;
         break;
        case 4: //klucz
         poziom_temp2[kratka] = 6;
         poziom_temp[kratka] = 7;
         break;
        case 5: //oczka
         poziom_temp2[kratka] = 20;
         poziom_temp[kratka] = 67;
         break;
        case 6: //dzialo obrotowe
         poziom_temp2[kratka] = 7;
         poziom_temp[kratka] = 38;
         break;
        case 7: //naboje
         poziom_temp2[kratka] = 20;
         poziom_temp[kratka] = 58;
         break;
        case 8: //ludzik
         poziom_temp2[kratka] = 20;
         poziom_temp[kratka] = 45;
         break;
        case 9: //kratka
         poziom_temp2[kratka] = 7;
         poziom_temp[kratka] = 24;
         break;
        case 10: //kapsula
         poziom_temp2[kratka] = 6;
         poziom_temp[kratka] = 5;
         break;
       }
      } else {
       poziom_temp2[kratka] = 6;
       poziom_temp[kratka] = poziom_shadow[kratka] + 1;
      }

     }

     if (((poziom_shadow[kratka]) >= 75) && ((poziom_shadow[kratka]) <= 81)) { //dym jakikolwiek
      if (poziom_shadow[kratka] == 81) {
       poziom_temp2[kratka] = 0;
       poziom_temp[kratka] = 0;
      } else {
       if ((poziom_shadow2[kratka]) != 6) {
        if ((poziom_shadow2[kratka]) == 1) { //jesli do gory
         if (((poziom_temp2[(kratka - 16)]) == 0) || ((poziom_temp2[(kratka - 16)]) > 7)) { //jesli do gory
          //jesli nie ma blokady niech plomien leci dalej
          poziom_temp2[(kratka - 16)] = 1;
          poziom_temp[(kratka - 16)] = 75;
         }
         //zamiana skierownaego plonienia miotacza na zwykly dym
         poziom_temp2[kratka] = 6;
         poziom_temp[kratka] = 76;
        }
        if ((poziom_shadow2[kratka]) == 2) { //jesli w prawo
         if (((poziom_temp2[(kratka + 1)]) == 0) || ((poziom_temp2[(kratka + 1)]) > 7)) { //jesli do gory
          //jesli nie ma blokady niech plomien leci dalej
          poziom_temp2[(kratka + 1)] = 2;
          poziom_temp[(kratka + 1)] = 75;
         }
         //zamiana skierownaego plonienia miotacza na zwykly dym
         poziom_temp2[kratka] = 6;
         poziom_temp[kratka] = 76;
        }
        if ((poziom_shadow2[kratka]) == 3) { //jesli do gory
         if (((poziom_temp2[(kratka + 16)]) == 0) || ((poziom_temp2[(kratka + 16)]) > 7)) { //jesli do gory
          //jesli nie ma blokady niech plomien leci dalej
          poziom_temp2[(kratka + 16)] = 3;
          poziom_temp[(kratka + 16)] = 75;
         }
         //zamiana skierownaego plonienia miotacza na zwykly dym
         poziom_temp2[kratka] = 6;
         poziom_temp[kratka] = 76;
        }
        if ((poziom_shadow2[kratka]) == 4) { //jesli w prawo
         if (((poziom_temp2[(kratka - 1)]) == 0) || ((poziom_temp2[(kratka - 1)]) > 7)) { //jesli do gory
          //jesli nie ma blokady niech plomien leci dalej
          poziom_temp2[(kratka - 1)] = 4;
          poziom_temp[(kratka - 1)] = 75;
         }
         //zamiana skierownaego plonienia miotacza na zwykly dym
         poziom_temp2[kratka] = 6;
         poziom_temp[kratka] = 76;
        }
       } else {
        poziom_temp2[kratka] = 6;
        poziom_temp[kratka] = poziom_shadow[kratka] + 1;
       }
      }
     }
     if (((poziom_shadow[kratka]) == 25) && ((poziom_shadow2[kratka]) <= 4)) { //jesli kratka ruchoma i sie porusza
      if (poziom_shadow2[kratka] == 1) {
       if ((poziom_temp2[(kratka - 16)]) == 0) { //jesli powyzej pole jest puste
        poziom_temp2[(kratka - 16)] = 1;
        poziom_temp[(kratka - 16)] = 25;
        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
       } else {
        if (((poziom_temp2[(kratka - 16)]) == 20) || ((poziom_temp2[(kratka - 16)]) == 21)) {
         if (sfx == 1) {
          try {
           s06.start();
          } catch (Exception ex) {}
         }

         if ((poziom_temp2[(kratka - 16)]) == 20) {
          poziom_temp2[(kratka - 16)] = 6;
          poziom_temp[(kratka - 16)] = 77;
         } else {
          poziom_temp2[(kratka - 16)] = 6;
          poziom_temp[(kratka - 16)] = 82;
         }
         poziom_temp2[kratka] = 7;
         poziom_temp[kratka] = 25;
        } else {
         if (((poziom_temp2[(kratka - 16)]) != 0) && ((poziom_temp2[(kratka - 16)]) != 20)) { //jesli pole powyzej ma blokade
          poziom_temp2[kratka] = 7;
          poziom_temp[kratka] = 25;
         }
        }
       }
      }



      if (poziom_shadow2[kratka] == 2) {
       if ((poziom_temp2[(kratka + 1)]) == 0) { //jesli po prawej pole jest puste
        poziom_temp2[(kratka + 1)] = 2;
        poziom_temp[(kratka + 1)] = 25;
        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
       } else {
        if (((poziom_temp2[(kratka + 1)]) == 20) || ((poziom_temp2[(kratka + 1)]) == 21)) {
         if (sfx == 1) {
          try {
           s06.start();
          } catch (Exception ex) {}
         }
         if ((poziom_temp2[(kratka + 1)]) == 20) {
          poziom_temp2[(kratka + 1)] = 6;
          poziom_temp[(kratka + 1)] = 77;
         } else {
          poziom_temp2[(kratka + 1)] = 6;
          poziom_temp[(kratka + 1)] = 82;
         }
         poziom_temp2[kratka] = 7;
         poziom_temp[kratka] = 25;
        } else {
         if (((poziom_temp2[(kratka + 1)]) != 0) && ((poziom_temp2[(kratka + 1)]) != 20)) { //jesli pole powyzej ma blokade
          poziom_temp2[kratka] = 7;
          poziom_temp[kratka] = 25;
         }
        }
       }
      }

      if (poziom_shadow2[kratka] == 3) {
       if ((poziom_temp2[(kratka + 16)]) == 0) { //jesli powyzej pole jest puste
        poziom_temp2[(kratka + 16)] = 3;
        poziom_temp[(kratka + 16)] = 25;
        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
       } else {
        if (((poziom_temp2[(kratka + 16)]) == 20) || ((poziom_temp2[(kratka + 16)]) == 21)) {
         if (sfx == 1) {
          try {
           s06.start();
          } catch (Exception ex) {}
         }
         if ((poziom_temp2[(kratka + 16)]) == 20) {
          poziom_temp2[(kratka + 16)] = 6;
          poziom_temp[(kratka + 16)] = 77;
         } else {
          poziom_temp2[(kratka + 16)] = 6;
          poziom_temp[(kratka + 16)] = 82;
         }
         poziom_temp2[kratka] = 7;
         poziom_temp[kratka] = 25;
        } else {
         if (((poziom_temp2[(kratka + 16)]) != 0) && ((poziom_temp2[(kratka + 16)]) != 20)) { //jesli pole powyzej ma blokade
          poziom_temp2[kratka] = 7;
          poziom_temp[kratka] = 25;
         }
        }
       }
      }


      if (poziom_shadow2[kratka] == 4) {
       if ((poziom_temp2[(kratka - 1)]) == 0) { //jesli powyzej pole jest puste
        poziom_temp2[(kratka - 1)] = 4;
        poziom_temp[(kratka - 1)] = 25;
        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
       } else {
        if (((poziom_temp2[(kratka - 1)]) == 20) || ((poziom_temp2[(kratka - 1)]) == 21)) {
         if (sfx == 1) {
          try {
           s06.start();
          } catch (Exception ex) {}
         }
         if ((poziom_temp2[(kratka - 1)]) == 20) {
          poziom_temp2[(kratka - 1)] = 6;
          poziom_temp[(kratka - 1)] = 77;
         } else {
          poziom_temp2[(kratka - 1)] = 6;
          poziom_temp[(kratka - 1)] = 82;
         }
         poziom_temp2[kratka] = 7;
         poziom_temp[kratka] = 25;
        } else {
         if (((poziom_temp2[(kratka - 1)]) != 0) && ((poziom_temp2[(kratka - 1)]) != 20)) { //jesli pole powyzej ma blokade
          poziom_temp2[kratka] = 7;
          poziom_temp[kratka] = 25;
         }
        }
       }
      }


     }

     if ((poziom_shadow[kratka]) == 26) { //dzialko zwykle gora
      if (losowa2(czestotliwosc, i, j) == 0) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka - 16)] == 0) {
        poziom_temp[(kratka - 16)] = 55;
        poziom_temp2[(kratka - 16)] = 1;
       }
       if ((poziom_temp2[(kratka - 16)]) == 20) { //jesli
        if (sfx == 1) {
         try {
          s06.start();
         } catch (Exception ex) {}
        }
        poziom_temp2[(kratka - 16)] = 6;
        poziom_temp[(kratka - 16)] = 77;
       }
       if ((poziom_temp2[(kratka - 16)]) == 21) { //jesli
        if (sfx == 1) {
         try {
          s06.start();
         } catch (Exception ex) {}
        }
        poziom_temp2[(kratka - 16)] = 6;
        poziom_temp[(kratka - 16)] = 82;
       }
       if ((poziom_shadow2[(kratka - 16)]) == 15) { //bomba
        eksplozja(i, j - 1);
       }

      }
     }
     if ((poziom_shadow[kratka]) == 27) { //dzialko zwykle prawo
      if (losowa2(czestotliwosc, i, j) == 1) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka + 1)] == 0) {
        poziom_temp[(kratka + 1)] = 53;
        poziom_temp2[(kratka + 1)] = 2;
       }
       if ((poziom_temp2[(kratka + 1)]) == 20) { //jesli
        if (sfx == 1) {
         try {
          s06.start();
         } catch (Exception ex) {}
        }
        poziom_temp2[(kratka + 1)] = 6;
        poziom_temp[(kratka + 1)] = 77;
       }
       if ((poziom_temp2[(kratka + 1)]) == 21) { //jesli
        if (sfx == 1) {
         try {
          s06.start();
         } catch (Exception ex) {}
        }
        poziom_temp2[(kratka + 1)] = 6;
        poziom_temp[(kratka + 1)] = 82;
       }
       if ((poziom_shadow2[(kratka + 1)]) == 15) { //bomba
        eksplozja(i + 1, j);
       }
      }
     }
     if ((poziom_shadow[kratka]) == 28) { //dzialko zwykle dol
      if (losowa2(czestotliwosc, i, j) == 2) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka + 16)] == 0) {
        poziom_temp[(kratka + 16)] = 55;
        poziom_temp2[(kratka + 16)] = 3;
       }
       if ((poziom_temp2[(kratka + 16)]) == 20) { //jesli
        if (sfx == 1) {
         try {
          s06.start();
         } catch (Exception ex) {}
        }
        poziom_temp2[(kratka + 16)] = 6;
        poziom_temp[(kratka + 16)] = 77;
       }
       if ((poziom_temp2[(kratka + 16)]) == 21) { //jesli
        if (sfx == 1) {
         try {
          s06.start();
         } catch (Exception ex) {}
        }
        poziom_temp2[(kratka + 16)] = 6;
        poziom_temp[(kratka + 16)] = 82;
       }
       if ((poziom_shadow2[(kratka + 16)]) == 15) { //bomba
        eksplozja(i, j + 1);
       }

      }
     }
     if ((poziom_shadow[kratka]) == 29) { //dzialko zwykle lewo
      if (losowa2(czestotliwosc, i, j) == 3) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka - 1)] == 0) {
        poziom_temp[(kratka - 1)] = 53;
        poziom_temp2[(kratka - 1)] = 4;
       }
       if ((poziom_temp2[(kratka - 1)]) == 20) { //jesli
        if (sfx == 1) {
         try {
          s06.start();
         } catch (Exception ex) {}
        }
        poziom_temp2[(kratka - 1)] = 6;
        poziom_temp[(kratka - 1)] = 77;
       }
       if ((poziom_temp2[(kratka - 1)]) == 21) { //jesli
        if (sfx == 1) {
         try {
          s06.start();
         } catch (Exception ex) {}
        }
        poziom_temp2[(kratka - 1)] = 6;
        poziom_temp[(kratka - 1)] = 82;
       }
       if ((poziom_shadow2[(kratka - 1)]) == 15) { //bomba
        eksplozja(i - 1, j);
       }
      }
     }

     if ((poziom_shadow[kratka]) == 30) { //dzialko ciagle gora
      if (losowa2(czestotliwosc, i, j) == 4) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka - 16)] == 0) {
        poziom_temp[(kratka - 16)] = 56;
        poziom_temp2[(kratka - 16)] = 1;
       }
       if ((poziom_temp2[(kratka - 16)]) == 20) { //jesli
        poziom_temp2[(kratka - 16)] = 6;
        poziom_temp[(kratka - 16)] = 77;
       }
       if ((poziom_temp2[(kratka - 16)]) == 21) { //jesli
        poziom_temp2[(kratka - 16)] = 6;
        poziom_temp[(kratka - 16)] = 82;
       }
       if ((poziom_shadow2[(kratka - 16)]) == 15) { //bomba
        eksplozja(i, j - 1);
       }

      }
     }
     if ((poziom_shadow[kratka]) == 31) { //dzialko ciagle prawo
      if (losowa2(czestotliwosc, i, j) == 5) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka + 1)] == 0) {
        poziom_temp[(kratka + 1)] = 54;
        poziom_temp2[(kratka + 1)] = 2;
       }
       if ((poziom_temp2[(kratka + 1)]) == 20) { //jesli
        poziom_temp2[(kratka + 1)] = 6;
        poziom_temp[(kratka + 1)] = 77;
       }
       if ((poziom_temp2[(kratka + 1)]) == 21) { //jesli
        poziom_temp2[(kratka + 1)] = 6;
        poziom_temp[(kratka + 1)] = 82;
       }
       if ((poziom_shadow2[(kratka + 1)]) == 15) { //bomba
        eksplozja(i + 1, j);
       }
      }
     }
     if ((poziom_shadow[kratka]) == 32) { //dzialko ciagle dol
      if (losowa2(czestotliwosc, i, j) == 6) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka + 16)] == 0) {
        poziom_temp[(kratka + 16)] = 56;
        poziom_temp2[(kratka + 16)] = 3;
       }
       if ((poziom_temp2[(kratka + 16)]) == 20) { //jesli
        poziom_temp2[(kratka + 16)] = 6;
        poziom_temp[(kratka + 16)] = 77;
       }
       if ((poziom_temp2[(kratka + 16)]) == 21) { //jesli
        poziom_temp2[(kratka + 16)] = 6;
        poziom_temp[(kratka + 16)] = 82;
       }
       if ((poziom_shadow2[(kratka + 16)]) == 15) { //bomba
        eksplozja(i, j + 1);
       }
      }
     }
     if ((poziom_shadow[kratka]) == 33) { //dzialko ciagle lewo
      if (losowa2(czestotliwosc, i, j) == 7) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka - 1)] == 0) {
        poziom_temp[(kratka - 1)] = 54;
        poziom_temp2[(kratka - 1)] = 4;
       }
       if ((poziom_temp2[(kratka - 1)]) == 20) { //jesli
        poziom_temp2[(kratka - 1)] = 6;
        poziom_temp[(kratka - 1)] = 77;
       }
       if ((poziom_temp2[(kratka - 1)]) == 21) { //jesli
        poziom_temp2[(kratka - 1)] = 6;
        poziom_temp[(kratka - 1)] = 82;
       }
       if ((poziom_shadow2[(kratka - 1)]) == 15) { //bomba
        eksplozja(i - 1, j);
       }
      }
     }



     if ((poziom_shadow[kratka]) == 34) { //dzialko miotacz gora
      if (losowa2(czestotliwosc, i, j) == 4) {
       //dzwiek strzalu
       if ((poziom_temp2[(kratka - 16)] != 6) && (poziom_temp2[(kratka - 16)] != 7)) {
        poziom_temp[(kratka - 16)] = 75;
        poziom_temp2[(kratka - 16)] = 1;
       }
      }
     }
     if ((poziom_shadow[kratka]) == 35) { //dzialko miotacz prawo
      if (losowa2(czestotliwosc, i, j) == 5) {
       //dzwiek strzalu
       if ((poziom_temp2[(kratka + 1)] != 6) && (poziom_temp2[(kratka + 1)] != 7)) {
        poziom_temp[(kratka + 1)] = 75;
        poziom_temp2[(kratka + 1)] = 2;
       }
      }
     }
     if ((poziom_shadow[kratka]) == 36) { //dzialko miotacz dol
      if (losowa2(czestotliwosc, i, j) == 6) {
       //dzwiek strzalu
       if ((poziom_temp2[(kratka + 16)] != 6) && (poziom_temp2[(kratka + 16)] != 7)) {
        poziom_temp[(kratka + 16)] = 75;
        poziom_temp2[(kratka + 16)] = 3;
       }
      }
     }
     if ((poziom_shadow[kratka]) == 37) { //dzialko miotacz lewo
      if (losowa2(czestotliwosc, i, j) == 7) {
       //dzwiek strzalu
       if ((poziom_temp2[(kratka - 1)] != 6) && (poziom_temp2[(kratka - 1)] != 7)) {
        poziom_temp[(kratka - 1)] = 75;
        poziom_temp2[(kratka - 1)] = 4;
       }
      }
     }


     if ((poziom_shadow[kratka]) == 38) { //dzialko obrotowe gora
      if (losowa2(czestotliwosc, i, j) == 8) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka - 16)] == 0) {
        poziom_temp[(kratka - 16)] = 55;
        poziom_temp2[(kratka - 16)] = 1;
       }
       if ((poziom_temp2[(kratka - 16)]) == 20) { //jesli
        poziom_temp2[(kratka - 16)] = 6;
        poziom_temp[(kratka - 16)] = 77;
       }
       if ((poziom_temp2[(kratka - 16)]) == 21) { //jesli
        poziom_temp2[(kratka - 16)] = 6;
        poziom_temp[(kratka - 16)] = 82;
       }
       if ((poziom_shadow2[(kratka - 16)]) == 15) { //bomba
        eksplozja(i, j - 1);
       }
      }
     }
     if ((poziom_shadow[kratka]) == 39) { //dzialko obrotowe prawo
      if (losowa2(czestotliwosc, i, j) == 9) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka + 1)] == 0) {
        poziom_temp[(kratka + 1)] = 53;
        poziom_temp2[(kratka + 1)] = 2;
       }
       if ((poziom_temp2[(kratka + 1)]) == 20) { //jesli
        poziom_temp2[(kratka + 1)] = 6;
        poziom_temp[(kratka + 1)] = 77;
       }
       if ((poziom_temp2[(kratka + 1)]) == 21) { //jesli
        poziom_temp2[(kratka + 1)] = 6;
        poziom_temp[(kratka + 1)] = 82;
       }
       if ((poziom_shadow2[(kratka + 1)]) == 15) { //bomba
        eksplozja(i + 1, j);
       }

      }
     }
     if ((poziom_shadow[kratka]) == 40) { //dzialko obrotowe dol
      if (losowa2(czestotliwosc, i, j) == 10) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka + 16)] == 0) {
        poziom_temp[(kratka + 16)] = 55;
        poziom_temp2[(kratka + 16)] = 3;
       }
       if ((poziom_temp2[(kratka + 16)]) == 20) { //jesli
        poziom_temp2[(kratka + 16)] = 6;
        poziom_temp[(kratka + 16)] = 77;
       }
       if ((poziom_temp2[(kratka + 16)]) == 21) { //jesli
        poziom_temp2[(kratka + 16)] = 6;
        poziom_temp[(kratka + 16)] = 82;
       }
       if ((poziom_shadow2[(kratka + 16)]) == 15) { //bomba
        eksplozja(i, j + 1);
       }
      }
     }
     if ((poziom_shadow[kratka]) == 41) { //dzialko obrotowe lewo
      if (losowa2(czestotliwosc, i, j) == 11) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka - 1)] == 0) {
        poziom_temp[(kratka - 1)] = 53;
        poziom_temp2[(kratka - 1)] = 4;
       }
       if ((poziom_temp2[(kratka - 1)]) == 20) { //jesli
        poziom_temp2[(kratka - 1)] = 6;
        poziom_temp[(kratka - 1)] = 77;
       }
       if ((poziom_temp2[(kratka - 1)]) == 21) { //jesli
        poziom_temp2[(kratka - 1)] = 6;
        poziom_temp[(kratka - 1)] = 82;
       }
       if ((poziom_shadow2[(kratka - 1)]) == 15) { //bomba
        eksplozja(i - 1, j);
       }

      }
     }

     if (((poziom_shadow[kratka]) >= 38) && ((poziom_shadow[kratka]) <= 41)) { //dzialko obrotowe
      if (losowa2(20, i, j) == 8) {
       tymcz1 = poziom_shadow[kratka];
       if (losowa2(40, i, j) < 21) {
        tymcz1--;
       } else {
        tymcz1++;
       }
       if (tymcz1 < 38)
        tymcz1 = 41;
       if (tymcz1 > 41)
        tymcz1 = 38;
       poziom_temp[kratka] = tymcz1;
      }
     } //dzialko obrotowe - obrot


     if (((poziom_shadow[kratka]) == 42) || ((poziom_shadow[kratka]) == 43)) { //dzialko obrotowe gora
      if (losowa2(czestotliwosc, i, j) == 11) {
       //dzwiek strzalu
       if (poziom_temp2[(kratka - 16)] == 0) {
        poziom_temp[(kratka - 16)] = 55;
        poziom_temp2[(kratka - 16)] = 1;
       }
       if ((poziom_temp2[(kratka - 16)]) == 20) { //jesli
        poziom_temp2[(kratka - 16)] = 6;
        poziom_temp[(kratka - 16)] = 77;
       }
       if ((poziom_temp2[(kratka - 16)]) == 21) { //jesli
        poziom_temp2[(kratka - 16)] = 6;
        poziom_temp[(kratka - 16)] = 82;
       }
       if ((poziom_shadow2[(kratka - 16)]) == 15) { //bomba
        eksplozja(i, j - 1);
       }
      }
      if ((poziom_shadow[kratka]) == 42) {
       if (klatka_sek == 0) {
        if (poziom_temp2[(kratka + 1)] == 0) {
         poziom_temp[(kratka + 1)] = 42;
         poziom_temp2[(kratka + 1)] = 7;
         poziom_temp[kratka] = 0;
         poziom_temp2[kratka] = 0;
        } else {
         poziom_temp[kratka] = 43;
         poziom_temp2[kratka] = 7;
        }
       }
      }
      if ((poziom_shadow[kratka]) == 43) {
       if (klatka_sek == 0) {
        if (poziom_temp2[(kratka - 1)] == 0) {
         poziom_temp[(kratka - 1)] = 43;
         poziom_temp2[(kratka - 1)] = 7;
         poziom_temp[kratka] = 0;
         poziom_temp2[kratka] = 0;
        } else {
         poziom_temp[kratka] = 42;
         poziom_temp2[kratka] = 7;
        }
       }
      }
     }





     if ((poziom_shadow[kratka]) == 53) { //naboj poziom 1
      if ((poziom_shadow2[kratka]) == 2) { //jesli leci w p
       if ((poziom_temp2[(kratka + 1)]) == 0) { //jesli
        poziom_temp2[(kratka + 1)] = 2;
        poziom_temp[(kratka + 1)] = 53;
        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
       } else {
        if ((poziom_temp2[(kratka + 1)]) == 21) { //znak zap
         poziom_temp2[(kratka + 1)] = 6;
         poziom_temp[(kratka + 1)] = 82;
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
        } else {
         if ((poziom_temp2[(kratka + 1)]) == 20) { //jesli
          poziom_temp2[(kratka + 1)] = 6;
          poziom_temp[(kratka + 1)] = 77;
          poziom_temp2[kratka] = 0;
          poziom_temp[kratka] = 0;
         } else {
          poziom_temp2[kratka] = 6;
          poziom_temp[kratka] = 79;
         }
        }
        if ((poziom_temp2[(kratka + 1)]) == 15) { //bomba
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
         eksplozja(i + 1, j);
        }

       }
      }
      if ((poziom_shadow2[kratka]) == 4) { //jesli leci w l
       if ((poziom_temp2[(kratka - 1)]) == 0) { //jesli
        poziom_temp2[(kratka - 1)] = 4;
        poziom_temp[(kratka - 1)] = 53;
        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
       } else {
        if ((poziom_temp2[(kratka - 1)]) == 21) { //jesli
         poziom_temp2[(kratka - 1)] = 6;
         poziom_temp[(kratka - 1)] = 82;
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
        } else {
         if ((poziom_temp2[(kratka - 1)]) == 20) { //jesli
          poziom_temp2[(kratka - 1)] = 6;
          poziom_temp[(kratka - 1)] = 77;
          poziom_temp2[kratka] = 0;
          poziom_temp[kratka] = 0;
         } else {
          poziom_temp2[kratka] = 6;
          poziom_temp[kratka] = 79;
         }
        }
        if ((poziom_temp2[(kratka - 1)]) == 15) { //bomba
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
         eksplozja(i - 1, j);
        }
       }
      }

     }
     if ((poziom_shadow[kratka]) == 54) { //naboj ciagly poziom 1
      if ((poziom_shadow2[kratka]) == 2) { //jesli leci w p
       if ((poziom_temp2[(kratka + 1)]) == 0) { //jesli
        poziom_temp2[(kratka + 1)] = 2;
        poziom_temp[(kratka + 1)] = 54;
        if (poziom_shadow[(kratka - 1)] == 51) {
         poziom_temp2[kratka] = 2;
        } else {
         poziom_temp2[kratka] = 5;
        }
        poziom_temp[kratka] = 51;
       } else {
        if ((poziom_temp2[(kratka + 1)]) == 21) { //jesli
         poziom_temp2[(kratka + 1)] = 6;
         poziom_temp[(kratka + 1)] = 82;
         poziom_temp2[kratka] = 4;
         poziom_temp[kratka] = 54;
        } else {
         if ((poziom_temp2[(kratka + 1)]) == 20) { //jesli
          poziom_temp2[(kratka + 1)] = 6;
          poziom_temp[(kratka + 1)] = 77;
          poziom_temp2[kratka] = 4;
          poziom_temp[kratka] = 54;
         } else {
          if (((poziom_temp2[(kratka + 1)]) != 0) && ((poziom_temp2[(kratka + 1)]) != 20)) { //jesli
           if ((poziom_shadow[(kratka + 1)]) == 51) {
            if (poziom_shadow2[(kratka + 1)] == 5) {
             poziom_temp2[(kratka + 1)] = 2;
             poziom_temp[(kratka + 1)] = 53;
             poziom_temp2[kratka] = 0;
             poziom_temp[kratka] = 0;
            } else {
             poziom_temp2[(kratka + 1)] = 2;
             poziom_temp[(kratka + 1)] = 54;
             poziom_temp2[kratka] = 0;
             poziom_temp[kratka] = 0;
            }
           } else {
            if (poziom_shadow[(kratka - 1)] == 51) {
             poziom_temp2[kratka] = 4;
             poziom_temp[kratka] = 54;
            } else {
             poziom_temp2[kratka] = 6;
             poziom_temp[kratka] = 79;
            }
           }
          }
         }
        }
        if ((poziom_temp2[(kratka + 1)]) == 15) { //bomba
         if (poziom_shadow[(kratka - 1)] == 51) {
          poziom_temp2[(kratka - 1)] = poziom_temp2[kratka];
          poziom_temp[(kratka - 1)] = poziom_temp[kratka];
         }
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
         eksplozja(i + 1, j);
        }

       }

      }
      if ((poziom_shadow2[kratka]) == 4) { //jesli leci w l
       if ((poziom_temp2[(kratka - 1)]) == 0) { //jesli
        poziom_temp2[(kratka - 1)] = 4;
        poziom_temp[(kratka - 1)] = 54;
        if (poziom_shadow[(kratka + 1)] == 51) {
         poziom_temp2[kratka] = 4;
        } else {
         poziom_temp2[kratka] = 5;
        }
        poziom_temp[kratka] = 51;
       } else {
        if ((poziom_temp2[(kratka - 1)]) == 21) { //jesli
         poziom_temp2[(kratka - 1)] = 6;
         poziom_temp[(kratka - 1)] = 82;
         poziom_temp2[kratka] = 2;
         poziom_temp[kratka] = 54;
        } else {
         if ((poziom_temp2[(kratka - 1)]) == 20) { //jesli
          poziom_temp2[(kratka - 1)] = 6;
          poziom_temp[(kratka - 1)] = 77;
          poziom_temp2[kratka] = 2;
          poziom_temp[kratka] = 54;
         } else {
          if (((poziom_temp2[(kratka - 1)]) != 0) && ((poziom_temp2[(kratka - 1)]) != 20)) { //jesli
           if ((poziom_shadow[(kratka - 1)]) == 51) {
            if (poziom_shadow2[(kratka - 1)] == 5) {
             poziom_temp2[(kratka - 1)] = 4;
             poziom_temp[(kratka - 1)] = 53;
             poziom_temp2[kratka] = 0;
             poziom_temp[kratka] = 0;
            } else {
             poziom_temp2[(kratka - 1)] = 4;
             poziom_temp[(kratka - 1)] = 54;
             poziom_temp2[kratka] = 0;
             poziom_temp[kratka] = 0;
            }
           } else {
            if (poziom_shadow[(kratka + 1)] == 51) {
             poziom_temp2[kratka] = 2;
             poziom_temp[kratka] = 54;
            } else {
             poziom_temp2[kratka] = 6;
             poziom_temp[kratka] = 79;
            }
           }
          }
         }
        }

        if ((poziom_temp2[(kratka - 1)]) == 15) { //bomba
         if (poziom_shadow[(kratka + 1)] == 51) {
          poziom_temp2[(kratka + 1)] = poziom_temp2[kratka];
          poziom_temp[(kratka + 1)] = poziom_temp[kratka];
         }
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
         eksplozja(i - 1, j);
        }

       }
      }

     }


     if ((poziom_shadow[kratka]) == 55) { //naboj pion 1
      if ((poziom_shadow2[kratka]) == 1) { //jesli leci do gory
       if ((poziom_temp2[(kratka - 16)]) == 0) { //jesli powyzej pole jest puste
        poziom_temp2[(kratka - 16)] = 1;
        poziom_temp[(kratka - 16)] = 55;
        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
       } else {
        if ((poziom_temp2[(kratka - 16)]) == 21) {
         poziom_temp2[(kratka - 16)] = 6;
         poziom_temp[(kratka - 16)] = 82;
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
        } else {
         if ((poziom_temp2[(kratka - 16)]) == 20) {
          poziom_temp2[(kratka - 16)] = 6;
          poziom_temp[(kratka - 16)] = 77;
          poziom_temp2[kratka] = 0;
          poziom_temp[kratka] = 0;
         } else {
          if (((poziom_temp2[(kratka - 16)]) != 0) && ((poziom_temp2[(kratka - 16)]) != 20)) { //jesli pole powyzej ma blokade
           poziom_temp2[kratka] = 6;
           poziom_temp[kratka] = 79;
          }
         }
        }
        if ((poziom_temp2[(kratka - 16)]) == 15) { //bomba
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
         eksplozja(i, j - 1);
        }

       }
      }
      if ((poziom_shadow2[kratka]) == 3) { //jesli leci do dolu
       if ((poziom_temp2[(kratka + 16)]) == 0) { //jesli ponizej pole jest puste
        poziom_temp2[(kratka + 16)] = 3;
        poziom_temp[(kratka + 16)] = 55;
        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
       } else {
        if ((poziom_temp2[(kratka + 16)]) == 21) { //jesli ponizej pole jest puste
         poziom_temp2[(kratka + 16)] = 6;
         poziom_temp[(kratka + 16)] = 82;
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
        } else {
         if ((poziom_temp2[(kratka + 16)]) == 20) { //jesli ponizej pole jest puste
          poziom_temp2[(kratka + 16)] = 6;
          poziom_temp[(kratka + 16)] = 77;
          poziom_temp2[kratka] = 0;
          poziom_temp[kratka] = 0;
         } else {
          if (((poziom_temp2[(kratka + 16)]) != 0) && ((poziom_temp2[(kratka + 16)]) != 20)) { //jesli pole ponizej ma blokade
           poziom_temp2[kratka] = 6;
           poziom_temp[kratka] = 79;
          }
         }
        }
        if ((poziom_temp2[(kratka + 16)]) == 15) { //bomba
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
         eksplozja(i, j + 1);
        }
       }
      }

     }


     if ((poziom_shadow[kratka]) == 56) { //naboj ciagly pion 1
      if ((poziom_shadow2[kratka]) == 1) { //jesli leci do gory
       if ((poziom_temp2[(kratka - 16)]) == 0) { //jesli powyzej pole jest puste
        poziom_temp2[(kratka - 16)] = 1;
        poziom_temp[(kratka - 16)] = 56;
        if (poziom_shadow[(kratka + 16)] == 52) {
         poziom_temp2[kratka] = 1;
        } else {
         poziom_temp2[kratka] = 5;
        }
        //							poziom_temp2[kratka]=1;
        poziom_temp[kratka] = 52;
       } else {
        if ((poziom_temp2[(kratka - 16)]) == 21) { //jesli powyzej pole jest puste
         poziom_temp2[(kratka - 16)] = 6;
         poziom_temp[(kratka - 16)] = 82;
         poziom_temp2[kratka] = 3;
         poziom_temp[kratka] = 56;
        } else {
         if ((poziom_temp2[(kratka - 16)]) == 20) { //jesli powyzej pole jest puste
          poziom_temp2[(kratka - 16)] = 6;
          poziom_temp[(kratka - 16)] = 77;
          poziom_temp2[kratka] = 3;
          poziom_temp[kratka] = 56;
         } else {
          if (((poziom_temp2[(kratka - 16)]) != 0) && ((poziom_temp2[(kratka - 16)]) != 20)) { //jesli pole powyzej ma blokade
           if ((poziom_shadow[(kratka - 16)]) == 52) {
            if (poziom_shadow2[(kratka - 16)] == 5) {
             poziom_temp2[(kratka - 16)] = 1;
             poziom_temp[(kratka - 16)] = 55;
             poziom_temp2[kratka] = 0;
             poziom_temp[kratka] = 0;
            } else {
             poziom_temp2[(kratka - 16)] = 1;
             poziom_temp[(kratka - 16)] = 56;
             poziom_temp2[kratka] = 0;
             poziom_temp[kratka] = 0;
            }
           } else {
            if (poziom_shadow[(kratka + 16)] == 52) {
             poziom_temp2[kratka] = 3;
             poziom_temp[kratka] = 56;
            } else {
             poziom_temp2[kratka] = 6;
             poziom_temp[kratka] = 79;
            }
           }
          }
         }
        }
        if ((poziom_temp2[(kratka - 16)]) == 15) { //bomba
         if (poziom_shadow[(kratka + 16)] == 52) {
          poziom_temp2[(kratka + 16)] = poziom_temp2[kratka];
          poziom_temp[(kratka + 16)] = poziom_temp[kratka];
         }
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
         eksplozja(i, j - 1);
        }

       }
      }
      if ((poziom_shadow2[kratka]) == 3) { //jesli leci do dolu
       if ((poziom_temp2[(kratka + 16)]) == 0) { //jesli ponizej pole jest puste
        poziom_temp2[(kratka + 16)] = 3;
        poziom_temp[(kratka + 16)] = 56;
        if (poziom_shadow[(kratka - 16)] == 52) {
         poziom_temp2[kratka] = 3;
        } else {
         poziom_temp2[kratka] = 5;
        }

        //							poziom_temp2[kratka]=3;
        poziom_temp[kratka] = 52;
       } else {
        if ((poziom_temp2[(kratka + 16)]) == 21) { //jesli ponizej pole jest puste
         poziom_temp2[(kratka + 16)] = 6;
         poziom_temp[(kratka + 16)] = 82;
         poziom_temp2[kratka] = 1;
         poziom_temp[kratka] = 56;
        } else {
         if ((poziom_temp2[(kratka + 16)]) == 20) { //jesli ponizej pole jest puste
          poziom_temp2[(kratka + 16)] = 6;
          poziom_temp[(kratka + 16)] = 77;
          poziom_temp2[kratka] = 1;
          poziom_temp[kratka] = 56;
         } else {
          if (((poziom_temp2[(kratka + 16)]) != 0) && ((poziom_temp2[(kratka + 16)]) != 20)) { //jesli pole ponizej ma blokade
           if ((poziom_shadow[(kratka + 16)]) == 52) {
            if (poziom_shadow2[(kratka + 16)] == 5) {
             poziom_temp2[(kratka + 16)] = 3;
             poziom_temp[(kratka + 16)] = 55;
             poziom_temp2[kratka] = 0;
             poziom_temp[kratka] = 0;
            } else {
             poziom_temp2[(kratka + 16)] = 3;
             poziom_temp[(kratka + 16)] = 56;
             poziom_temp2[kratka] = 0;
             poziom_temp[kratka] = 0;
            }
           } else {
            if (poziom_shadow[(kratka - 16)] == 52) {
             poziom_temp2[kratka] = 1;
             poziom_temp[kratka] = 56;
            } else {
             poziom_temp2[kratka] = 6;
             poziom_temp[kratka] = 79;
            }
           }
          }
         }
        }
        if ((poziom_temp2[(kratka + 16)]) == 15) { //bomba
         if (poziom_shadow[(kratka - 16)] == 52) {
          poziom_temp2[(kratka - 16)] = poziom_temp2[kratka];
          poziom_temp[(kratka - 16)] = poziom_temp[kratka];
         }
         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
         eksplozja(i, j + 1);
        }

       }
      }

     }
    }
    //			 }

   }
  }
  System.arraycopy(poziom_temp2, 0, poziom_shadow2, 0, 1120);
  System.arraycopy(poziom_temp, 0, poziom_shadow, 0, 1120);
 }

 //wyzerowac_przy nowym lvl:
 //robbo_dead

 private void poruszpostacie() {
  int kratka;
  int znaleziono = 0;
  int znaleziono1 = 0;
  int tymcz1, tymcz2, tymcz5;
  System.arraycopy(poziom_shadow2, 0, poziom_temp2, 0, 1120);
  System.arraycopy(poziom_shadow, 0, poziom_temp, 0, 1120);
  int magnes = 0;
  int robbo_dead = 0;
  int ruch_oczka = 0;

  for (i = 0; i < 16; i++) {
   if ((poziom_shadow[i + (robot_y * 16)] >= 59) && (poziom_shadow[i + (robot_y * 16)] <= 66) && (magnes == 1)) {
    robbo_dead = 4;
   }

   if (poziom_shadow2[i + (robot_y * 16)] != 0)
    magnes = 0;
   if (poziom_shadow[i + (robot_y * 16)] == 9)
    magnes = 1;
  }

  for (i = 15; i >= 0; i--) {
   if ((poziom_shadow[i + (robot_y * 16)] >= 59) && (poziom_shadow[i + (robot_y * 16)] <= 66) && (magnes == 1)) {
    robbo_dead = 2;
   }

   if (poziom_shadow2[i + (robot_y * 16)] != 0)
    magnes = 0;
   if (poziom_shadow[i + (robot_y * 16)] == 10)
    magnes = 1;
  }
  if (robbo_dead > 0) {
   kierunek = 0;
  }

  for (j = 0; j < 70; j++) {
   for (i = 0; i < 16; i++) {
    kratka = i + (j * 16);
    if (poziom_shadow[kratka] > 10) {
     if (bufor > 0) {
      if ((poziom_shadow[kratka] >= 11) && (poziom_shadow[kratka] <= 22) && ((i != bufor_x) || (j != bufor_y))) {
       tymcz1 = (poziom_shadow[kratka] - 9) / 2;
       if (tymcz1 == bufor) {
        wolne[1] = 1;
        wolne[2] = 1;
        wolne[3] = 1;
        wolne[4] = 1;
        if (poziom_shadow2[(kratka - 16)] == 0)
         wolne[1] = 0;
        if (poziom_shadow2[(kratka + 1)] == 0)
         wolne[2] = 0;
        if (poziom_shadow2[(kratka + 16)] == 0)
         wolne[3] = 0;
        if (poziom_shadow2[(kratka - 1)] == 0)
         wolne[4] = 0;
        tymcz1 = 0;
        while (tymcz1 <= 3) {
         tymcz2 = bufor_kier + tymcz1;
         if (tymcz2 > 4)
          tymcz2 = tymcz2 - 4;
         if ((wolne[tymcz2] == 0) && (bufor > 0)) {
          if (tymcz2 == 1) {
           poziom_temp[(kratka - 16)] = 59;
           poziom_temp2[(kratka - 16)] = 20;
           bufor = 0;
          }
          if (tymcz2 == 2) {
           poziom_temp[(kratka + 1)] = 61;
           poziom_temp2[(kratka + 1)] = 20;
           bufor = 0;
          }
          if (tymcz2 == 3) {
           poziom_temp[(kratka + 16)] = 63;
           poziom_temp2[(kratka + 16)] = 20;
           bufor = 0;
          }
          if (tymcz2 == 4) {
           poziom_temp[(kratka - 1)] = 65;
           poziom_temp2[(kratka - 1)] = 20;
           bufor = 0;
          }
         }
         tymcz1++;
        }
       }
      }
     }
     if (((poziom_shadow[kratka]) >= 59) && ((poziom_shadow[kratka]) <= 66) && (znaleziono == 0)) { //robbo
      if (robbo_dead == 2) {
       if ((poziom_shadow2[(kratka + 1)]) == 0) { //jesli po pr puste
        poziom_temp2[(kratka + 1)] = 20;

        poziom_temp[(kratka + 1)] = poziom_shadow[kratka];

        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
       } else {
        poziom_temp2[kratka] = 6;
        poziom_temp[kratka] = 75;
       }
      }
      if (robbo_dead == 4) {
       if ((poziom_shadow2[(kratka - 1)]) == 0) { //jesli po l puste
        poziom_temp2[(kratka - 1)] = 20;

        poziom_temp[(kratka - 1)] = poziom_shadow[kratka];

        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
       } else {
        poziom_temp2[kratka] = 6;
        poziom_temp[kratka] = 75;
       }
      }
      znaleziono = 1; //odhaczyc dla malej nieregularnej oszczednosci predkosci
      robot_x = i;
      robot_y = j;
      posdoc_y = (j - (wysokosc / 2)) * 8;
      if (pos_y < 0) {
       pos_y = 0;
      } //kamera

      if ((kierunek == 1) && (fire != 0) && (fire_buf == 0)) {
       bufor_f = -1;
       poziom_temp[kratka] = 60;
       fire_buf = 1;
       if (naboje > 0) {
        if (poziom_shadow2[(kratka - 16)] == 0) { //jesli to pustka
         poziom_temp[(kratka - 16)] = 55;
         poziom_temp2[(kratka - 16)] = 1;
        }
        if ((poziom_shadow2[(kratka - 16)]) == 20) { //jesli jest zniszczalne
         poziom_temp2[(kratka - 16)] = 6;
         poziom_temp[(kratka - 16)] = 77;
        }
        if ((poziom_shadow2[(kratka - 16)]) == 21) { //jesli jest to znak zapytania
         poziom_temp2[(kratka - 16)] = 6;
         poziom_temp[(kratka - 16)] = 82;
        }
        if ((poziom_shadow2[(kratka - 16)]) == 15) { //jesli to bomba
         eksplozja(i, j - 1);
        }
        naboje--;
       }
      }
      if ((kierunek == 2) && (fire != 0) && (fire_buf == 0)) {
       bufor_f = -1;
       poziom_temp[kratka] = 62;
       fire_buf = 1;
       if (naboje > 0) {
        if (poziom_shadow2[(kratka + 1)] == 0) { //jesli to pustka
         poziom_temp[(kratka + 1)] = 53;
         poziom_temp2[(kratka + 1)] = 2;
        }
        if ((poziom_shadow2[(kratka + 1)]) == 20) { //jesli jest zniszczalne
         poziom_temp2[(kratka + 1)] = 6;
         poziom_temp[(kratka + 1)] = 77;
        }
        if ((poziom_shadow2[(kratka + 1)]) == 21) { //jesli jest to znak zapytania
         poziom_temp2[(kratka + 1)] = 6;
         poziom_temp[(kratka + 1)] = 82;
        }
        if ((poziom_shadow2[(kratka + 1)]) == 15) { //jesli to bomba
         eksplozja(i + 1, j);
        }
        naboje--;
       }
      }
      if ((kierunek == 3) && (fire != 0) && (fire_buf == 0)) {
       bufor_f = -1;
       poziom_temp[kratka] = 64;
       fire_buf = 1;
       if (naboje > 0) {
        if (poziom_shadow2[(kratka + 16)] == 0) { //jesli to pustka
         poziom_temp[(kratka + 16)] = 55;
         poziom_temp2[(kratka + 16)] = 3;
        }
        if ((poziom_shadow2[(kratka + 16)]) == 20) { //jesli jest zniszczalne
         poziom_temp2[(kratka + 16)] = 6;
         poziom_temp[(kratka + 16)] = 77;
        }
        if ((poziom_shadow2[(kratka + 16)]) == 21) { //jesli jest to znak zapytania
         poziom_temp2[(kratka + 16)] = 6;
         poziom_temp[(kratka + 16)] = 82;
        }
        if ((poziom_shadow2[(kratka + 16)]) == 15) { //jesli to bomba
         eksplozja(i, j + 1);
        }
        naboje--;
       }
      }
      if ((kierunek == 4) && (fire != 0) && (fire_buf == 0)) {
       bufor_f = -1;
       poziom_temp[kratka] = 66;
       fire_buf = 1;
       if (naboje > 0) {
        if (poziom_shadow2[(kratka - 1)] == 0) { //jesli to pustka
         poziom_temp[(kratka - 1)] = 53;
         poziom_temp2[(kratka - 1)] = 4;
        }
        if ((poziom_shadow2[(kratka - 1)]) == 20) { //jesli jest zniszczalne
         poziom_temp2[(kratka - 1)] = 6;
         poziom_temp[(kratka - 1)] = 77;
        }
        if ((poziom_shadow2[(kratka - 1)]) == 21) { //jesli jest to znak zapytania
         poziom_temp2[(kratka - 1)] = 6;
         poziom_temp[(kratka - 1)] = 82;
        }
        if ((poziom_shadow2[(kratka - 1)]) == 15) { //jesli to bomba
         eksplozja(i - 1, j);
        }
        naboje--;
       }
      }


      if ((kierunek == 1) && (fire == 0)) {
       if ((poziom_temp2[(kratka - 16)]) == 0) { //jesli powyzej pole jest puste
        robot_y = j - 1;
        poziom_temp2[(kratka - 16)] = 20;

        if (poziom_shadow[kratka] == 59) {
         poziom_temp[(kratka - 16)] = 60;
        } else {
         poziom_temp[(kratka - 16)] = 59;
        }

        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
        if (sfx == 1) {
         try {
          s04.stop();
          s04.start();
         } catch (Exception ex) {}
        }
       }
       if (zbieralne(poziom_temp[(kratka - 16)]) == 1) {
        robot_y = j - 1;
        //dodac item do kieszeni
        if (poziom_temp[(kratka - 16)] == 7) {
         //odglos klucza
         klucze++;
         if (sfx == 1) {
          try {
           s03.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka - 16)] == 8) {
         //odglos srubki
         srubki--;
         if (srubki < 0) {
          srubki = 0;
         }
         if (sfx == 1) {
          try {
           s07.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka - 16)] == 58) {
         //odglos nabojow
         naboje = naboje + 9;
         if (naboje > 99)
          naboje = 99;
         if (sfx == 1) {
          try {
           s14.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka - 16)] == 45) {
         //odglos zycia
         zycia++;
         if (sfx == 1) {
          try {
           s09.start();
          } catch (Exception ex) {}
         }
        }

        poziom_temp2[(kratka - 16)] = 20;

        if (poziom_shadow[kratka] == 59) {
         poziom_temp[(kratka - 16)] = 60;
        } else {
         poziom_temp[(kratka - 16)] = 59;
        }

        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;

        if ((poziom_shadow[(kratka - 16)] == 5) || (poziom_shadow[(kratka - 16)] == 6)) {
         poziom_temp2[(kratka - 16)] = 0;
         poziom_temp[(kratka - 16)] = 0;
         koniec_poziomu = 1;
        }

       } else {
        if ((((poziom_temp[(kratka - 16)]) == 24) || ((poziom_temp[(kratka - 16)]) == 25) || ((poziom_temp[(kratka - 16)]) == 46) || ((poziom_temp[(kratka - 16)]) == 44) || ((poziom_temp[(kratka - 16)]) == 42) || ((poziom_temp[(kratka - 16)]) == 43) || ((poziom_temp[(kratka - 16)]) == 47)) && ((poziom_temp2[i + ((j - 2) * 16)]) == 0)) {
         robot_y = j - 1;
         poziom_temp[i + ((j - 2) * 16)] = poziom_shadow[(kratka - 16)];
         if (poziom_temp[(kratka - 16)] == 25) { //jesli przesuwajaca sie
          poziom_temp2[i + ((j - 2) * 16)] = 1;
         } //to nadaj kierunek
         else { // jesli nie
          poziom_temp2[i + ((j - 2) * 16)] = 6;
         } //nie nadawaj kierunku

         poziom_temp2[(kratka - 16)] = 20;

         if (poziom_shadow[kratka] == 59) {
          poziom_temp[(kratka - 16)] = 60;
         } else {
          poziom_temp[(kratka - 16)] = 59;
         }

         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
        } else {
         if ((poziom_temp[(kratka - 16)] >= 11) && (poziom_temp[(kratka - 16)] <= 22)) { //jesli to teleport
          robot_x = 0;
          robot_y = 0;
          bufor = (poziom_shadow[(kratka - 16)] - 9) / 2;
          bufor_kier = kierunek;
          bufor_x = i;
          bufor_y = j - 1;
          poziom_temp2[kratka] = 0;
          poziom_temp[kratka] = 0;
          if (sfx == 1) {
           try {
            s12.stop();
            s12.start();
           } catch (Exception ex) {}
          }
         } else {
          if (((poziom_temp2[(kratka - 16)]) > 0) && ((poziom_temp2[(kratka - 16)]) <= 7)) { //jesli pole powyzej ma blokade
           if (poziom_shadow[kratka] == 59) {
            poziom_temp[kratka] = 60;
           } else {
            poziom_temp[kratka] = 59;
           }
           if ((poziom_temp[(kratka - 16)] == 23) && (klucze > 0)) {
            //odglos otwieranych drzwi
            if (sfx == 1) {
             try {
              s13.start();
             } catch (Exception ex) {}
            }
            klucze--;
            poziom_temp[(kratka - 16)] = 0;
            poziom_temp2[(kratka - 16)] = 0;
           }
          }
         }
        }
       }
      }

      if ((kierunek == 2) && (fire == 0)) {
       if ((poziom_temp2[(kratka + 1)]) == 0) { //jesli po pr puste
        robot_x = i + 1;
        poziom_temp2[(kratka + 1)] = 20;

        if (poziom_shadow[kratka] == 61) {
         poziom_temp[(kratka + 1)] = 62;
        } else {
         poziom_temp[(kratka + 1)] = 61;
        }

        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
        if (sfx == 1) {
         try {
          s04.stop();
          s04.start();
         } catch (Exception ex) {}
        }
       }
       if (zbieralne(poziom_temp[(kratka + 1)]) == 1) {
        robot_x = i + 1;
        //dodac zbieralny item do kieszeni

        //dodac item do kieszeni
        if (poziom_temp[(kratka + 1)] == 7) {
         //odglos klucza
         klucze++;
         if (sfx == 1) {
          try {
           s03.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka + 1)] == 8) {
         //odglos srubki
         srubki--;
         if (srubki < 0) {
          srubki = 0;
         }
         if (sfx == 1) {
          try {
           s07.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka + 1)] == 58) {
         //odglos nabojow
         naboje = naboje + 9;
         if (naboje > 99)
          naboje = 99;
         if (sfx == 1) {
          try {
           s14.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka + 1)] == 45) {
         //odglos zycia
         zycia++;
         if (sfx == 1) {
          try {
           s09.start();
          } catch (Exception ex) {}
         }
        }
        poziom_temp2[(kratka + 1)] = 20;

        if (poziom_shadow[kratka] == 61) {
         poziom_temp[(kratka + 1)] = 62;
        } else {
         poziom_temp[(kratka + 1)] = 61;
        }

        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;

        if ((poziom_shadow[(kratka + 1)] == 5) || (poziom_shadow[(kratka + 1)] == 6)) {
         poziom_temp2[(kratka + 1)] = 0;
         poziom_temp[(kratka + 1)] = 0;
         koniec_poziomu = 1;
        }
       } else {
        if ((((poziom_temp[(kratka + 1)]) == 24) || ((poziom_temp[(kratka + 1)]) == 25) || ((poziom_temp[(kratka + 1)]) == 46) || ((poziom_temp[(kratka + 1)]) == 44) || ((poziom_temp[(kratka + 1)]) == 42) || ((poziom_temp[(kratka + 1)]) == 43) || ((poziom_temp[(kratka + 1)]) == 47)) && ((poziom_temp2[i + 2 + ((j) * 16)]) == 0)) {
         robot_x = i + 1;
         poziom_temp[i + 2 + ((j) * 16)] = poziom_shadow[(kratka + 1)];
         if (poziom_temp[(kratka + 1)] == 25) { //jesli przesuwajaca sie
          poziom_temp2[i + 2 + ((j) * 16)] = 2;
         } //to nadaj kierunek
         else { // jesli nie
          poziom_temp2[i + 2 + ((j) * 16)] = 6;
         } //nie nadawaj kierunku

         poziom_temp2[(kratka + 1)] = 20;

         if (poziom_shadow[kratka] == 61) {
          poziom_temp[(kratka + 1)] = 62;
         } else {
          poziom_temp[(kratka + 1)] = 61;
         }

         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
        } else {
         if ((poziom_temp[(kratka + 1)] >= 11) && (poziom_temp[(kratka + 1)] <= 22)) { //jesli to teleport
          robot_x = 0;
          robot_y = 0;
          bufor = (poziom_shadow[(kratka + 1)] - 9) / 2;
          bufor_kier = kierunek;
          bufor_x = i + 1;
          bufor_y = j;
          poziom_temp2[kratka] = 0;
          poziom_temp[kratka] = 0;
          if (sfx == 1) {
           try {
            s12.stop();
            s12.start();
           } catch (Exception ex) {}
          }
         } else {
          if (((poziom_temp2[(kratka + 1)]) > 0) && ((poziom_temp2[(kratka + 1)]) <= 7)) { //jesli pole po pr ma blok
           if (poziom_shadow[kratka] == 61) {
            poziom_temp[kratka] = 62;
           } else {
            poziom_temp[kratka] = 61;
           }
           if ((poziom_temp[(kratka + 1)] == 23) && (klucze > 0)) {
            //odglos otwieranych drzwi
            if (sfx == 1) {
             try {
              s13.start();
             } catch (Exception ex) {}
            }
            klucze--;
            poziom_temp[(kratka + 1)] = 0;
            poziom_temp2[(kratka + 1)] = 0;
           }
          }
         }
        }
       }
      }


      if ((kierunek == 3) && (fire == 0)) {
       if ((poziom_temp2[(kratka + 16)]) == 0) { //jesli ponizej pusto
        robot_y = j + 1;
        poziom_temp2[(kratka + 16)] = 20;

        if (poziom_shadow[kratka] == 63) {
         poziom_temp[(kratka + 16)] = 64;
        } else {
         poziom_temp[(kratka + 16)] = 63;
        }

        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
        if (sfx == 1) {
         try {
          s04.stop();
          s04.start();
         } catch (Exception ex) {}
        }
       }
       if (zbieralne(poziom_temp[(kratka + 16)]) == 1) {
        robot_y = j + 1;

        //dodac item do kieszeni
        if (poziom_temp[(kratka + 16)] == 7) {
         //odglos klucza
         klucze++;
         if (sfx == 1) {
          try {
           s03.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka + 16)] == 8) {
         //odglos srubki
         srubki--;
         if (srubki < 0) {
          srubki = 0;
         }
         if (sfx == 1) {
          try {
           s07.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka + 16)] == 58) {
         //odglos nabojow
         naboje = naboje + 9;
         if (naboje > 99)
          if (sfx == 1) {
           try {
            s14.start();
           } catch (Exception ex) {}
          }
        }
        if (poziom_temp[(kratka + 16)] == 45) {
         //odglos zycia
         zycia++;
         if (sfx == 1) {
          try {
           s09.start();
          } catch (Exception ex) {}
         }
        }
        //dodaj do kieszeni
        poziom_temp2[(kratka + 16)] = 20;

        if (poziom_shadow[kratka] == 63) {
         poziom_temp[(kratka + 16)] = 64;
        } else {
         poziom_temp[(kratka + 16)] = 63;
        }

        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;


        if ((poziom_shadow[(kratka + 16)] == 5) || (poziom_shadow[(kratka + 16)] == 6)) {
         poziom_temp2[(kratka + 16)] = 0;
         poziom_temp[(kratka + 16)] = 0;
         koniec_poziomu = 1;
        }


       } else {
        if ((((poziom_temp[(kratka + 16)]) == 24) || ((poziom_temp[(kratka + 16)]) == 25) || ((poziom_temp[(kratka + 16)]) == 46) || ((poziom_temp[(kratka + 16)]) == 44) || ((poziom_temp[(kratka + 16)]) == 42) || ((poziom_temp[(kratka + 16)]) == 43) || ((poziom_temp[(kratka + 16)]) == 47)) && ((poziom_temp2[i + ((j + 2) * 16)]) == 0)) {
         robot_y = j + 1;
         poziom_temp[i + ((j + 2) * 16)] = poziom_shadow[(kratka + 16)];
         if (poziom_temp[(kratka + 16)] == 25) { //jesli przesuwajaca sie
          poziom_temp2[i + ((j + 2) * 16)] = 3;
         } //to nadaj kierunek
         else { // jesli nie
          poziom_temp2[i + ((j + 2) * 16)] = 6;
         } //nie nadawaj kierunku

         poziom_temp2[(kratka + 16)] = 20;

         if (poziom_shadow[kratka] == 63) {
          poziom_temp[(kratka + 16)] = 64;
         } else {
          poziom_temp[(kratka + 16)] = 63;
         }

         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;
        } else {
         if ((poziom_temp[(kratka + 16)] >= 11) && (poziom_temp[(kratka + 16)] <= 22)) { //jesli to teleport
          robot_x = 0;
          robot_y = 0;
          bufor = (poziom_shadow[(kratka + 16)] - 9) / 2;
          bufor_kier = kierunek;
          bufor_x = i;
          bufor_y = j + 1;
          poziom_temp2[kratka] = 0;
          poziom_temp[kratka] = 0;
          if (sfx == 1) {
           try {
            s12.stop();
            s12.start();
           } catch (Exception ex) {}
          }
         } else {
          if (((poziom_temp2[(kratka + 16)]) > 0) && ((poziom_temp2[(kratka + 16)]) <= 7)) { //jesli ponizej bokada
           if (poziom_shadow[kratka] == 63) {
            poziom_temp[kratka] = 64;
           } else {
            poziom_temp[kratka] = 63;
           }
           if ((poziom_temp[(kratka + 16)] == 23) && (klucze > 0)) {
            //odglos otwieranych drzwi
            if (sfx == 1) {
             try {
              s13.start();
             } catch (Exception ex) {}
            }
            klucze--;
            poziom_temp[(kratka + 16)] = 0;
            poziom_temp2[(kratka + 16)] = 0;
           }
          }
         }
        }
       }
      }

      if ((kierunek == 4) && (fire == 0)) {
       if ((poziom_temp2[(kratka - 1)]) == 0) { //jesli po lw puste
        robot_x = i - 1;
        poziom_temp2[(kratka - 1)] = 20;

        if (poziom_shadow[kratka] == 65) {
         poziom_temp[(kratka - 1)] = 66;
        } else {
         poziom_temp[(kratka - 1)] = 65;
        }

        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
        if (sfx == 1) {
         try {
          s04.stop();
          s04.start();
         } catch (Exception ex) {}
        }
       }
       if (zbieralne(poziom_temp[(kratka - 1)]) == 1) {
        robot_x = i - 1;
        //dodac do kieszeni
        if (poziom_temp[(kratka - 1)] == 7) {
         //odglos klucza
         klucze++;
         if (sfx == 1) {
          try {
           s03.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka - 1)] == 8) {
         //odglos srubki
         srubki--;
         if (srubki < 0) {
          srubki = 0;
         }
         if (sfx == 1) {
          try {
           s07.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka - 1)] == 58) {
         //odglos nabojow
         naboje = naboje + 9;
         if (naboje > 99)
          naboje = 99;
         if (sfx == 1) {
          try {
           s14.start();
          } catch (Exception ex) {}
         }
        }
        if (poziom_temp[(kratka - 1)] == 45) {
         //odglos zycia
         zycia++;
         if (sfx == 1) {
          try {
           s09.start();
          } catch (Exception ex) {}
         }
        }

        poziom_temp2[(kratka - 1)] = 20;

        if (poziom_shadow[kratka] == 65) {
         poziom_temp[(kratka - 1)] = 66;
        } else {
         poziom_temp[(kratka - 1)] = 65;
        }

        poziom_temp2[kratka] = 0;
        poziom_temp[kratka] = 0;
        if ((poziom_shadow[(kratka - 1)] == 5) || (poziom_shadow[(kratka - 1)] == 6)) {
         poziom_temp2[(kratka - 1)] = 0;
         poziom_temp[(kratka - 1)] = 0;
         koniec_poziomu = 1;
        }
       } else {
        if ((((poziom_temp[(kratka - 1)]) == 24) || ((poziom_temp[(kratka - 1)]) == 25) || ((poziom_temp[(kratka - 1)]) == 46) || ((poziom_temp[(kratka - 1)]) == 44) || ((poziom_temp[(kratka - 1)]) == 42) || ((poziom_temp[(kratka - 1)]) == 43) || ((poziom_temp[(kratka - 1)]) == 47)) && ((poziom_temp2[i - 2 + ((j) * 16)]) == 0)) {
         robot_x = i - 1;
         poziom_temp[i - 2 + ((j) * 16)] = poziom_shadow[(kratka - 1)];
         if (poziom_temp[(kratka - 1)] == 25) { //jesli przesuwajaca sie
          poziom_temp2[i - 2 + ((j) * 16)] = 4;
         } //to nadaj kierunek
         else { // jesli nie
          poziom_temp2[i - 2 + ((j) * 16)] = 6;
         } //nie nadawaj kierunku

         poziom_temp2[(kratka - 1)] = 20;

         if (poziom_shadow[kratka] == 65) {
          poziom_temp[(kratka - 1)] = 66;
         } else {
          poziom_temp[(kratka - 1)] = 65;
         }

         poziom_temp2[kratka] = 0;
         poziom_temp[kratka] = 0;

        } else {
         if ((poziom_temp[(kratka - 1)] >= 11) && (poziom_temp[(kratka - 1)] <= 22)) { //jesli to teleport
          robot_x = 0;
          robot_y = 0;
          bufor = (poziom_shadow[(kratka - 1)] - 9) / 2;
          bufor_kier = kierunek;
          bufor_x = i - 1;
          bufor_y = j;
          poziom_temp2[kratka] = 0;
          poziom_temp[kratka] = 0;
          if (sfx == 1) {
           try {
            s12.stop();
            s12.start();
           } catch (Exception ex) {}
          }
         } else {
          if (((poziom_temp2[(kratka - 1)]) > 0) && ((poziom_temp2[(kratka - 1)]) <= 7)) { //jesli pole po lw ma blok
           if (poziom_shadow[kratka] == 65) {
            poziom_temp[kratka] = 66;
           } else {
            poziom_temp[kratka] = 65;
           }
           if ((poziom_temp[(kratka - 1)] == 23) && (klucze > 0)) {
            //odglos otwieranych drzwi
            if (sfx == 1) {
             try {
              s13.start();
             } catch (Exception ex) {}
            }
            klucze--;
            poziom_temp[(kratka - 1)] = 0;
            poziom_temp2[(kratka - 1)] = 0;
           }
          }
         }
        }
       }
      }

     }
     //POTWORKI

     if (((poziom_shadow[kratka]) == 105) || ((poziom_shadow[kratka]) == 106)) { //potworek r gora
      if (poziom_temp2[(kratka + 1)] == 0) { //jesli po prawej pusto
       poziom_temp[(kratka + 1)] = poziom_shadow[kratka] + 2; //obroc w prawo
       poziom_temp2[(kratka + 1)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else { //jesli po prawej jest sciana
       if (poziom_temp2[(kratka - 16)] == 0) { //jesli powyzej pusto
        poziom_temp[(kratka - 16)] = poziom_shadow[kratka];
        poziom_temp2[(kratka - 16)] = poziom_shadow2[kratka];
        poziom_temp[kratka] = 0;
        poziom_temp2[kratka] = 0;
       } else { //jesli powyzej jest sciana
        poziom_temp[kratka] = poziom_temp[kratka] + 6; //obroc w prawo
       }
      }
     }
     if (((poziom_shadow[kratka]) == 107) || ((poziom_shadow[kratka]) == 108)) { //potworek r prawo
      if (poziom_temp2[(kratka + 16)] == 0) { //jesli po prawej pusto
       //obroc w pr
       poziom_temp[(kratka + 16)] = poziom_shadow[kratka] + 2;
       poziom_temp2[(kratka + 16)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else { //jesli po lewej jest sciana
       if (poziom_temp2[(kratka + 1)] == 0) { //jesli powyzej pusto
        poziom_temp[(kratka + 1)] = poziom_shadow[kratka];
        poziom_temp2[(kratka + 1)] = poziom_shadow2[kratka];
        poziom_temp[kratka] = 0;
        poziom_temp2[kratka] = 0;
       } else { //jesli powyzej jest sciana
        poziom_temp[kratka] = poziom_temp[kratka] - 2; //obroc w lewo
       }
      }
     }
     if (((poziom_shadow[kratka]) == 109) || ((poziom_shadow[kratka]) == 110)) { //potworek r dol
      if (poziom_temp2[(kratka - 1)] == 0) { //jesli po pr pusto
       //obroc w pr
       poziom_temp[(kratka - 1)] = poziom_shadow[kratka] + 2;
       poziom_temp2[(kratka - 1)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else { //jesli po lewej jest sciana
       if (poziom_temp2[(kratka + 16)] == 0) { //jesli powyzej pusto
        poziom_temp[(kratka + 16)] = poziom_shadow[kratka];
        poziom_temp2[(kratka + 16)] = poziom_shadow2[kratka];
        poziom_temp[kratka] = 0;
        poziom_temp2[kratka] = 0;
       } else { //jesli powyzej jest sciana
        poziom_temp[kratka] = poziom_temp[kratka] - 2; //obroc w lw
       }
      }
     }
     if (((poziom_shadow[kratka]) == 111) || ((poziom_shadow[kratka]) == 112)) { //potworek r lewo
      if (poziom_temp2[(kratka - 16)] == 0) { //jesli po pr pusto
       //						//obroc w pr
       poziom_temp[(kratka - 16)] = poziom_shadow[kratka] - 6;
       poziom_temp2[(kratka - 16)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else { //jesli po lewej jest sciana
       if (poziom_temp2[(kratka - 1)] == 0) { //jesli powyzej pusto
        poziom_temp[(kratka - 1)] = poziom_shadow[kratka];
        poziom_temp2[(kratka - 1)] = poziom_shadow2[kratka];
        poziom_temp[kratka] = 0;
        poziom_temp2[kratka] = 0;
       } else { //jesli powyzej jest sciana
        poziom_temp[kratka] = poziom_temp[kratka] - 2; //obroc w lw
       }
      }
     }




     if (((poziom_shadow[kratka]) == 97) || ((poziom_shadow[kratka]) == 98)) { //potworek l gora
      if (poziom_temp2[(kratka - 1)] == 0) { //jesli po lewej pusto
       //						poziom_temp[kratka]=poziom_temp[kratka]+6; //obroc w lewo
       poziom_temp[(kratka - 1)] = poziom_shadow[kratka] + 6;
       poziom_temp2[(kratka - 1)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else { //jesli po lewej jest sciana
       if (poziom_temp2[(kratka - 16)] == 0) { //jesli powyzej pusto
        poziom_temp[(kratka - 16)] = poziom_shadow[kratka];
        poziom_temp2[(kratka - 16)] = poziom_shadow2[kratka];
        poziom_temp[kratka] = 0;
        poziom_temp2[kratka] = 0;
       } else { //jesli powyzej jest sciana
        poziom_temp[kratka] = poziom_temp[kratka] + 2; //obroc w prawo
       }
      }
     }
     //komentarze sa wzgledem aktualnego rucho potworka (czyli obroc w lewo dla ponizszego przykladu oznacza obroc w gore bezwzglednie)
     if (((poziom_shadow[kratka]) == 99) || ((poziom_shadow[kratka]) == 100)) { //potworek l prawo
      if (poziom_temp2[(kratka - 16)] == 0) { //jesli po lewej pusto
       //						poziom_temp[kratka]=poziom_temp[kratka]-2; //obroc w lewo
       poziom_temp[(kratka - 16)] = poziom_shadow[kratka] - 2;
       poziom_temp2[(kratka - 16)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else { //jesli po lewej jest sciana
       if (poziom_temp2[(kratka + 1)] == 0) { //jesli powyzej pusto
        poziom_temp[(kratka + 1)] = poziom_shadow[kratka];
        poziom_temp2[(kratka + 1)] = poziom_shadow2[kratka];
        poziom_temp[kratka] = 0;
        poziom_temp2[kratka] = 0;
       } else { //jesli powyzej jest sciana
        poziom_temp[kratka] = poziom_temp[kratka] + 2; //obroc w prawo
       }
      }
     }
     if (((poziom_shadow[kratka]) == 101) || ((poziom_shadow[kratka]) == 102)) { //potworek l dol
      if (poziom_temp2[(kratka + 1)] == 0) { //jesli po lewej pusto
       //						poziom_temp[kratka]=poziom_temp[kratka]-2; //obroc w lewo
       poziom_temp[(kratka + 1)] = poziom_shadow[kratka] - 2;
       poziom_temp2[(kratka + 1)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else { //jesli po lewej jest sciana
       if (poziom_temp2[(kratka + 16)] == 0) { //jesli powyzej pusto
        poziom_temp[(kratka + 16)] = poziom_shadow[kratka];
        poziom_temp2[(kratka + 16)] = poziom_shadow2[kratka];
        poziom_temp[kratka] = 0;
        poziom_temp2[kratka] = 0;
       } else { //jesli powyzej jest sciana
        poziom_temp[kratka] = poziom_temp[kratka] + 2; //obroc w prawo
       }
      }
     }
     if (((poziom_shadow[kratka]) == 103) || ((poziom_shadow[kratka]) == 104)) { //potworek l lewo
      if (poziom_temp2[(kratka + 16)] == 0) { //jesli po lewej pusto
       //						poziom_temp[kratka]=poziom_temp[kratka]-2; //obroc w lewo
       poziom_temp[(kratka + 16)] = poziom_shadow[kratka] - 2;
       poziom_temp2[(kratka + 16)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else { //jesli po lewej jest sciana
       if (poziom_temp2[(kratka - 1)] == 0) { //jesli powyzej pusto
        poziom_temp[(kratka - 1)] = poziom_shadow[kratka];
        poziom_temp2[(kratka - 1)] = poziom_shadow2[kratka];
        poziom_temp[kratka] = 0;
        poziom_temp2[kratka] = 0;
       } else { //jesli powyzej jest sciana
        poziom_temp[kratka] = poziom_temp[kratka] - 6; //obroc w prawo
       }
      }
     }




     if (((poziom_shadow[kratka]) >= 69) && ((poziom_shadow[kratka]) <= 70)) { //nietoperz st lewo
      if (poziom_temp2[(kratka - 1)] == 0) {
       poziom_temp[(kratka - 1)] = poziom_shadow[kratka];
       poziom_temp2[(kratka - 1)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else {
       poziom_temp[kratka] = (poziom_shadow[kratka]) + 2;
      }
     }

     if (((poziom_shadow[kratka]) >= 71) && ((poziom_shadow[kratka]) <= 72)) { //nietoperz st prawo
      if (poziom_temp2[(kratka + 1)] == 0) {
       poziom_temp[(kratka + 1)] = poziom_shadow[kratka];
       poziom_temp2[(kratka + 1)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else {
       poziom_temp[kratka] = (poziom_shadow[kratka]) - 2;
      }
     }


     if (((poziom_shadow[kratka]) >= 89) && ((poziom_shadow[kratka]) <= 90)) { //nietoperz lewo
      if (poziom_temp2[(kratka - 1)] == 0) {
       poziom_temp[(kratka - 1)] = poziom_shadow[kratka];
       poziom_temp2[(kratka - 1)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else {
       poziom_temp[kratka] = (poziom_shadow[kratka]) + 2;
      }
     }

     if (((poziom_shadow[kratka]) >= 91) && ((poziom_shadow[kratka]) <= 92)) { //nietoperz prawo
      if (poziom_temp2[(kratka + 1)] == 0) {
       poziom_temp[(kratka + 1)] = poziom_shadow[kratka];
       poziom_temp2[(kratka + 1)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else {
       poziom_temp[kratka] = (poziom_shadow[kratka]) - 2;
      }
     }


     if (((poziom_shadow[kratka]) >= 93) && ((poziom_shadow[kratka]) <= 94)) { //nietoperz gora
      if (poziom_temp2[(kratka - 16)] == 0) {
       poziom_temp[(kratka - 16)] = poziom_shadow[kratka];
       poziom_temp2[(kratka - 16)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else {
       poziom_temp[kratka] = (poziom_shadow[kratka]) + 2;
      }
     }

     if (((poziom_shadow[kratka]) >= 95) && ((poziom_shadow[kratka]) <= 96)) { //nietoperz dol
      if (poziom_temp2[(kratka + 16)] == 0) {
       poziom_temp[(kratka + 16)] = poziom_shadow[kratka];
       poziom_temp2[(kratka + 16)] = poziom_shadow2[kratka];
       poziom_temp[kratka] = 0;
       poziom_temp2[kratka] = 0;
      } else {
       poziom_temp[kratka] = (poziom_shadow[kratka]) - 2;
      }
     }


     if (((poziom_shadow[kratka]) >= 67) && ((poziom_shadow[kratka]) <= 68)) { //oczka
      ruch_oczka = losowa2(99, i, j);

      if (ruch_oczka > 49) {
       if ((robot_x > i)) {
        tymcz5 = losowa2(10, i, j);
        if (tymcz5 > 2) {
         if (poziom_temp2[(kratka + 1)] == 0) {
          poziom_temp[(kratka + 1)] = poziom_shadow[kratka];
          poziom_temp2[(kratka + 1)] = poziom_shadow2[kratka];
          poziom_temp[kratka] = 0;
          poziom_temp2[kratka] = 0;
         }
        } else {
         if (tymcz5 < 1) {
          if (poziom_temp2[(kratka - 1)] == 0) {
           poziom_temp[(kratka - 1)] = poziom_shadow[kratka];
           poziom_temp2[(kratka - 1)] = poziom_shadow2[kratka];
           poziom_temp[kratka] = 0;
           poziom_temp2[kratka] = 0;
          }
         }
        }
       } else {
        if (robot_x < i) {
         tymcz5 = losowa2(10, i, j);
         if (tymcz5 > 2) {
          if (poziom_temp2[(kratka - 1)] == 0) {
           poziom_temp[(kratka - 1)] = poziom_shadow[kratka];
           poziom_temp2[(kratka - 1)] = poziom_shadow2[kratka];
           poziom_temp[kratka] = 0;
           poziom_temp2[kratka] = 0;
          }
         } else {
          if (tymcz5 < 1) {
           if (poziom_temp2[(kratka + 1)] == 0) {
            poziom_temp[(kratka + 1)] = poziom_shadow[kratka];
            poziom_temp2[(kratka + 1)] = poziom_shadow2[kratka];
            poziom_temp[kratka] = 0;
            poziom_temp2[kratka] = 0;
           }
          }
         }
        } else { //gdy jest na poziomie robota
         tymcz5 = losowa2(10, i, j);
         if (tymcz5 > 9) {
          if (poziom_temp2[(kratka - 1)] == 0) {
           poziom_temp[(kratka - 1)] = poziom_shadow[kratka];
           poziom_temp2[(kratka - 1)] = poziom_shadow2[kratka];
           poziom_temp[kratka] = 0;
           poziom_temp2[kratka] = 0;
          }
         } else {
          if (tymcz5 < 1) {
           if (poziom_temp2[(kratka + 1)] == 0) {
            poziom_temp[(kratka + 1)] = poziom_shadow[kratka];
            poziom_temp2[(kratka + 1)] = poziom_shadow2[kratka];
            poziom_temp[kratka] = 0;
            poziom_temp2[kratka] = 0;
           }
          }
         }

        }
       }
      } else {
       if ((robot_y > j)) {
        tymcz5 = losowa2(10, i, j);
        if (tymcz5 > 2) {
         if (poziom_temp2[(kratka + 16)] == 0) {
          poziom_temp[(kratka + 16)] = poziom_shadow[kratka];
          poziom_temp2[(kratka + 16)] = poziom_shadow2[kratka];
          poziom_temp[kratka] = 0;
          poziom_temp2[kratka] = 0;
         }
        } else {
         if (tymcz5 < 1) {
          if (poziom_temp2[(kratka - 16)] == 0) {
           poziom_temp[(kratka - 16)] = poziom_shadow[kratka];
           poziom_temp2[(kratka - 16)] = poziom_shadow2[kratka];
           poziom_temp[kratka] = 0;
           poziom_temp2[kratka] = 0;
          }
         }
        }
       } else {
        if (robot_y < j) {
         tymcz5 = losowa2(10, i, j);
         if (tymcz5 > 2) {
          if (poziom_temp2[(kratka - 16)] == 0) {
           poziom_temp[(kratka - 16)] = poziom_shadow[kratka];
           poziom_temp2[(kratka - 16)] = poziom_shadow2[kratka];
           poziom_temp[kratka] = 0;
           poziom_temp2[kratka] = 0;
          }
         } else {
          if (tymcz5 < 1) {
           if (poziom_temp2[(kratka + 16)] == 0) {
            poziom_temp[(kratka + 16)] = poziom_shadow[kratka];
            poziom_temp2[(kratka + 16)] = poziom_shadow2[kratka];
            poziom_temp[kratka] = 0;
            poziom_temp2[kratka] = 0;
           }
          }
         }
        } else { //gdy jest na poziomie robota
         tymcz5 = losowa2(10, i, j);
         if (tymcz5 > 9) {
          if (poziom_temp2[(kratka - 16)] == 0) {
           poziom_temp[(kratka - 16)] = poziom_shadow[kratka];
           poziom_temp2[(kratka - 16)] = poziom_shadow2[kratka];
           poziom_temp[kratka] = 0;
           poziom_temp2[kratka] = 0;
          }
         } else {
          if (tymcz5 < 1) {
           if (poziom_temp2[(kratka + 16)] == 0) {
            poziom_temp[(kratka + 16)] = poziom_shadow[kratka];
            poziom_temp2[(kratka + 16)] = poziom_shadow2[kratka];
            poziom_temp[kratka] = 0;
            poziom_temp2[kratka] = 0;
           }
          }
         }

        }
       }
      }

     }




    }
   }


   if ((robot_x > 0) && (robot_y > 0)) {
    if (zabija_r(poziom_temp[robot_x + ((robot_y - 1) * 16)]) == 1) { //powyzej
     poziom_temp[robot_x + ((robot_y) * 16)] = 75;
     poziom_temp2[robot_x + ((robot_y) * 16)] = 6;
     robot_x = 0;
     robot_y = 0;
     if (sfx == 1) {
      try {
       s06.realize();
       s06.start();
      } catch (Exception ex) {}
     }
    } else {
     if (zabija_r(poziom_temp[robot_x - 1 + ((robot_y) * 16)]) == 1) { //po lewej
      poziom_temp[robot_x + ((robot_y) * 16)] = 75;
      poziom_temp2[robot_x + ((robot_y) * 16)] = 6;
      robot_x = 0;
      robot_y = 0;
      if (sfx == 1) {
       try {
        s06.realize();
        s06.start();
       } catch (Exception ex) {}
      }
     } else {
      if (zabija_r(poziom_temp[robot_x + 1 + ((robot_y) * 16)]) == 1) { //po prawej
       poziom_temp[robot_x + ((robot_y) * 16)] = 75;
       poziom_temp2[robot_x + ((robot_y) * 16)] = 6;
       robot_x = 0;
       robot_y = 0;
       if (sfx == 1) {
        try {
         s06.realize();
         s06.start();
        } catch (Exception ex) {}
       }
      } else {
       if (zabija_r(poziom_temp[robot_x + ((robot_y + 1) * 16)]) == 1) { //ponizej
        poziom_temp[robot_x + ((robot_y) * 16)] = 75;
        poziom_temp2[robot_x + ((robot_y) * 16)] = 6;
        robot_x = 0;
        robot_y = 0;
        if (sfx == 1) {
         try {
          s06.realize();
          s06.start();
         } catch (Exception ex) {}
        }
       }
      }
     }
    }
   }
  }


  System.arraycopy(poziom_temp2, 0, poziom_shadow2, 0, 1120);
  System.arraycopy(poziom_temp, 0, poziom_shadow, 0, 1120);

  bufor_x = 0; //ustawione "niemozliwe" wartosci - powod przy deklaracji
  bufor_y = 0;
  fire = 0;
  if ((znaleziono == 0) && (restart == 0) && (bufor == 0)) {
   restart = 40;
  }
  if (znaleziono == 1) {
   restart = 0;
  }
 }


 private int zabija_r(int numer) {
  int tak = 0;
  if ((numer >= 67) && (numer <= 74)) {
   tak = 1;
  }
  if ((numer >= 89) && (numer <= 112)) {
   tak = 1;
  }
  return tak;
 }

 private int zbieralne(int numer) {
  int tak = 0;
  if ((numer == 5) || (numer == 6) || (numer == 7) || (numer == 8) || (numer == 58) || (numer == 45)) {
   tak = 1;
  }
  return tak;
 }

 private int losowa(int numer) {
  Random random = new Random();
  int tak = Math.abs(random.nextInt() % numer);
  return tak;
 }

 private int losowa2(int numer, int zm1, int zm2) {
  Random random = new Random();
  int tak = Math.abs((random.nextInt() + zm1 + zm2) % numer);
  return tak;
 }

 private void eksplozja(int p_x, int p_y) {
  int tymcz1, tymcz2;
  int tymcz3 = 0;
  for (tymcz1 = 0; tymcz1 < 3; tymcz1++) {
   for (tymcz2 = 0; tymcz2 < 3; tymcz2++) {
    if ((poziom_temp2[p_x - 1 + tymcz1 + ((p_y - 1 + tymcz2) * 16)] == 15) && ((tymcz1 != 1) || (tymcz2 != 1))) {
     poziom_temp[p_x - 1 + tymcz1 + ((p_y - 1 + tymcz2) * 16)] = 48;
    } else {
     if (((poziom_temp2[p_x - 1 + tymcz1 + ((p_y - 1 + tymcz2) * 16)] == 0) || (poziom_temp2[p_x - 1 + tymcz1 + ((p_y - 1 + tymcz2) * 16)] > 6))) {
      poziom_temp2[p_x - 1 + tymcz1 + ((p_y - 1 + tymcz2) * 16)] = 6;
      poziom_temp[p_x - 1 + tymcz1 + ((p_y - 1 + tymcz2) * 16)] = 75 + losowa(1) + tymcz3;
      tymcz3 = tymcz3 + 1;
      if (tymcz3 > 2)
       tymcz3 = 0;
     }
    }
   }
  }
  if (sfx == 1) {
   try {
    s11.start();
   } catch (Exception ex) {}
  }
 }

 public int il_pozi(String fpath) {
  int tymcz, tymcz2;
  int zwrot = 0;
  try {

   InputStream is = this.getClass().getResourceAsStream(fpath);
   DataInputStream ds = new DataInputStream(is);
   for (tymcz = 0; tymcz < 4; tymcz++) {
    byte bajt = ds.readByte();
    tymcz2 = (int) bajt;
    tymcz2 = tymcz2 - 48;
    if (tymcz == 0) {
     zwrot = zwrot + tymcz2 * (1000);
    }
    if (tymcz == 1) {
     zwrot = zwrot + tymcz2 * (100);
    }
    if (tymcz == 2) {
     zwrot = zwrot + tymcz2 * (10);
    }
    if (tymcz == 3) {
     zwrot = zwrot + tymcz2 * (1);
    }
   }

  } catch (Exception ex) {
   System.err.println("loading error : " + ex.getMessage());
  }
  return zwrot;
 }

 public void zaladujpoziom(String fpath) {
  int tymcz_x, tymcz_y;
  try {
   // open the file
   InputStream is = this.getClass().getResourceAsStream(fpath);
   DataInputStream ds = new DataInputStream(is);
   try {
    // skip the descriptor
    ds.skipBytes(17);


    for (tymcz_y = 0; tymcz_y < 31; tymcz_y++) {
     for (tymcz_x = 0; tymcz_x < 16; tymcz_x++) {
      // read a tile index
      byte bajt = ds.readByte();
      poziom_shadow[tymcz_x + (tymcz_y * 16)] = (int) bajt;
      int pozsha = -1;
      switch (poziom_shadow[tymcz_x + (tymcz_y * 16)]) {
       case 32:
        pozsha = 0;
        break;
       case 120:
        pozsha = 1;
        break;
       case 77:
        pozsha = 2;
        break;
       case 61:
        pozsha = 7;
        break;
       case 36:
        pozsha = 8;
        break;
       case 40:
        pozsha = 9;
        break;
       case 41:
        pozsha = 10;
        break;
       case 48:
        pozsha = 21;
        break;
       case 49:
        pozsha = 11;
        break;
       case 50:
        pozsha = 13;
        break;
       case 51:
        pozsha = 15;
        break;
       case 52:
        pozsha = 17;
        break;
       case 53:
        pozsha = 19;
        break;
       case 54:
        pozsha = 21;
        break;
       case 55:
        pozsha = 21;
        break;
       case 56:
        pozsha = 21;
        break;
       case 57:
        pozsha = 21;
        break;
       case 68:
        pozsha = 23;
        break;

       case 35:
        pozsha = 24;
        break;
       case 111:
        pozsha = 25;
        break;
       case 117:
        pozsha = 26;
        break;
       case 125:
        pozsha = 27;
        break;
       case 100:
        pozsha = 28;
        break;
       case 123:
        pozsha = 29;
        break;
       case 94:
        pozsha = 30;
        break;
       case 62:
        pozsha = 31;
        break;
       case 113:
        pozsha = 32;
        break;
       case 60:
        pozsha = 33;
        break;
       case 103:
        pozsha = 34;
        break;
       case 91:
        pozsha = 35;
        break;
       case 104:
        pozsha = 36;
        break;
       case 93:
        pozsha = 37;
        break;

       case 122:
        pozsha = 38;
        break;
       case 45:
        pozsha = 42;
        break;
       case 63:
        pozsha = 44;
        break;
       case 64:
        pozsha = 46;
        break;
       case 75:
        pozsha = 47;
        break;
       case 37:
        pozsha = 57;
        break;
       case 33:
        pozsha = 58;
        break;
       case 42:
        pozsha = 63;
        break;
       case 38:
        pozsha = 67;
        break;
       case 107:
        pozsha = 69;
        break;
       case 106:
        pozsha = 71;
        break;

       case 118:
        pozsha = 89;
        break;
       case 115:
        pozsha = 91;
        break;
       case 97:
        pozsha = 93;
        break;
       case 102:
        pozsha = 95;
        break;
       case 65:
        pozsha = 97;
        break;
       case 69:
        pozsha = 105;
        break;
      }
      if (pozsha >= 0) {
       poziom_shadow[tymcz_x + (tymcz_y * 16)] = pozsha;
      } else {
       poziom_shadow[tymcz_x + (tymcz_y * 16)] = 2;
      }

     }
     ds.skipBytes(2);

    }
    for (tymcz_y = 31; tymcz_y < 70; tymcz_y++) {
     for (tymcz_x = 0; tymcz_x < 16; tymcz_x++) {
      poziom_shadow[tymcz_x + (tymcz_y * 16)] = 1;
     }
    }

   } catch (Exception ex) {
    System.err.println("map loading error : " + ex.getMessage());
   }
   // close the file
   ds.close();
   ds = null;
   is = null;
  } catch (Exception ex) {
   System.err.println("map loading error : " + ex.getMessage());
  }

 }

}
