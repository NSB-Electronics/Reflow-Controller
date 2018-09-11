/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.reflowcontroller.UpdateReadings;

import java.awt.Toolkit;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author nicholas
 */
public class UpdateReadings {
    Toolkit toolkit;

  Timer timer;

  public UpdateReadings(int seconds) {
    toolkit = Toolkit.getDefaultToolkit();
    timer = new Timer();
    timer.schedule(new UpdateReadingsTask(), 0, seconds * 1000);
  }

  class UpdateReadingsTask extends TimerTask {
    public void run() {
      System.out.println("Time's up!");
      toolkit.beep();

      //timer.cancel(); //Not necessary because we call System.exit
      //System.exit(0); //Stops the AWT thread (and everything else)
    }
  }
}
