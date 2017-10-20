/* ==================================================================
 * TaskStepTracker.java - 20/10/2017 6:52:07 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.nim.util;

/**
 * Utility for tracking progress for a task that requires several discrete steps of possibly unknown
 * quantities of work.
 * 
 * <p>
 * This is designed to provide a way to give rough "percent complete" feedback during long-running
 * tasks.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class TaskStepTracker {

  private final int stepCount;
  private int currentStep = 0;
  private double stepPercentComplete = 0f;
  private String message = null;
  private Long startedDate = null;
  private Long completedDate = null;

  /**
   * Constructor.
   * 
   * @param stepCount
   *          the number of steps the task requires
   */
  public TaskStepTracker(int stepCount) {
    super();
    if (stepCount < 1) {
      throw new IllegalArgumentException("Step count must be > 0");
    }
    this.stepCount = stepCount;
  }

  /**
   * Complete the current step and start progress on the next step.
   * 
   * <p>
   * This will increment the current step until the {@code stepCount} is exceeded. At that point
   * {@code percent} will be forced to {@literal 0}.
   * </p>
   * 
   * @param percent
   *          the percent complete for the <b>next</b> step, between {@literal 0} and {@literal 1}
   */
  public synchronized void startNextStep(double percent) {
    if (currentStep <= stepCount) {
      currentStep += 1;
      if (currentStep > stepCount) {
        stepPercentComplete = 0;
      } else {
        stepPercentComplete = percent;
      }
    }
  }

  /**
   * Complete the current step.
   * 
   * <p>
   * This will increment the current step and reset the step percent complete to {@literal 0}.
   * </p>
   */
  public synchronized void completeStep() {
    if (currentStep < 1) {
      currentStep = 1;
    }
    startNextStep(0);
  }

  /**
   * Set the percent complete for the current step.
   * 
   * <p>
   * Call this during work on a single step within the task if the percent complete for that step
   * can be calculated.
   * </p>
   * 
   * @param percent
   *          the percent compelte for the current step, between {@literal 0} and {@literal 1}
   */
  public synchronized void setStepPercentComplete(double percent) {
    if (currentStep < 1) {
      currentStep = 1;
    }
    if (percent < 0f) {
      percent = 0f;
    } else if (percent > 1f) {
      percent = 1;
    }
    stepPercentComplete = percent;
  }

  /**
   * Get the overall percent complete.
   * 
   * @return the overall percent complete, between {@literal 0} and {@literal 1}
   */
  public synchronized double getOverallPercentComplete() {
    if (currentStep < 1) {
      return 0f;
    } else if (currentStep > stepCount) {
      return 1f;
    }
    return (double) (currentStep - 1) / (double) stepCount + stepPercentComplete / stepCount;
  }

  /**
   * Set a status message.
   * 
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Get a status message.
   * 
   * @param message
   *          the message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Get the started flag.
   * 
   * @return the started flag
   */
  public boolean isStarted() {
    return startedDate != null;
  }

  /**
   * Get the date {@link #start()} was called.
   * 
   * @return the start date
   */
  public Long getStartedDate() {
    return startedDate;
  }

  /**
   * Set the started flag to {@literal true}.
   */
  public void start() {
    this.startedDate = System.currentTimeMillis();
  }

  /**
   * Get the completed flag.
   * 
   * @return the completed flag
   */
  public boolean isCompleted() {
    return completedDate != null;
  }

  /**
   * Get the date {@link #complete()} was called.
   * 
   * @return the completed date
   */
  public Long getCompletedDate() {
    return completedDate;
  }

  /**
   * Set the complete flag to {@literal true}.
   */
  public void complete() {
    this.completedDate = System.currentTimeMillis();
  }

}
