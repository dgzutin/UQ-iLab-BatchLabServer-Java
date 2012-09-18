/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uq.ilabs.library.labserver.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import org.w3c.dom.Document;
import uq.ilabs.library.lab.types.*;
import uq.ilabs.library.lab.utilities.Delay;
import uq.ilabs.library.lab.utilities.Logfile;
import uq.ilabs.library.lab.utilities.XmlUtilities;
import uq.ilabs.library.labserver.engine.types.ExperimentResultInfo;
import uq.ilabs.library.labserver.engine.types.ExperimentStatisticInfo;
import uq.ilabs.library.labserver.engine.types.LabExperimentInfo;
import uq.ilabs.library.labserver.engine.types.QueuedExperimentInfo;

/**
 *
 * @author uqlpayne
 */
public class LabExperimentManager implements Runnable {

    //<editor-fold defaultstate="collapsed" desc="Constants">
    private static final String STR_ClassName = LabExperimentManager.class.getName();
    private static final Level logLevel = Level.FINE;
    /*
     * String constants for logfile messages
     */
    private static final String STRLOG_ExperimentIdSbName_arg2 = "ExperimentId: %d SbName: %s";
    private static final String STRLOG_UserGroup_arg = "UserGroup: %s";
    private static final String STRLOG_UserGroupPriorityHint_arg2 = "UserGroup: %s PriorityHint: %d";
    private static final String STRLOG_ExperimentStatus_arg3 = "StatusCode: %s  EstRuntime: %.0f  RemainingRuntime: %.0f";
    private static final String STRLOG_ExperimentStatus_arg4 = "QueuePosition: %d  QueueWaitTime: %.0f  EstRuntime: %.0f  RemainingRuntime: %.0f";
    private static final String STRLOG_StatusCode_arg = "StatusCode: %s";
    private static final String STRLOG_Accepted_arg = "Accepted: %s";
    private static final String STRLOG_UnitIdLabStatusMessage_arg2 = "%d:%s  ";
    private static final String STRLOG_StateChange_arg2 = "[LEM: %s->%s]";
    private static final String STRLOG_RevertToWaiting_arg = " Revert to waiting - ExperimentId: %d  SbName: '%s'";
    //
    protected static final String STRLOG_Success_arg = "Success: %s";
    /*
     * String constants for exception messages
     */
    private static final String STRERR_ThreadFailedToStart = "Thread failed to start!";
    private static final String STRERR_LabExperimentEngines = "LabExperimentEngine[]";
    private static final String STRERR_LabExperimentEngineUnitId_arg = "LabExperimentEngine[%d]";
    private static final String STRERR_FailedToEnqueueExperiment = "Failed to enqueue experiment!";
    /*
     * Constants
     */
    private static final int INT_DelayCheckQueueSeconds = 15;
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Variables">
    private Thread thread;
    private boolean stopRunning;
    //
    protected LabManagement labManagement;
    protected int farmSize;
    protected LabExperimentEngine[] labExperimentEngines;
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Properties">
    private String labServerGuid;
    private boolean online;
    private String labStatusMessage;
    private boolean running;

    public String getLabServerGuid() {
        return labServerGuid;
    }

    public boolean isOnline() {
        return online;
    }

    public String getLabStatusMessage() {
        return labStatusMessage;
    }

    public boolean isRunning() {
        return running;
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Types">

    private enum States {

        Init, Idle, CheckQueue, StartEngine, CheckNotified, Maintenance, StopRunning, Done
    }
    //</editor-fold>

    /**
     *
     * @param configPropertiesFilename
     * @throws Exception
     */
    public LabExperimentManager(LabManagement labManagement) throws Exception {
        final String methodName = "LabExperimentManager";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        try {
            /*
             * Check that parameters are valid
             */
            if (labManagement == null) {
                throw new NullPointerException(LabManagement.class.getSimpleName());
            }

            /*
             * Save to local variables
             */
            this.labManagement = labManagement;

            /*
             * Initialise locals
             */
            this.stopRunning = false;
            this.farmSize = this.labManagement.getLabEquipmentServiceInfo().length;
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
            throw ex;
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName);
    }

    /**
     *
     * @return
     */
    public boolean Create() {
        final String methodName = "Create";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        boolean success = false;

        try {
            /*
             * Create local class instances just to check that all is in order
             */
            LabExperimentResult labExperimentResult = new LabExperimentResult(this.labManagement.getLabConfiguration());
            if (labExperimentResult == null) {
                throw new NullPointerException(LabExperimentResult.class.getSimpleName());
            }

            /*
             * Create instances of the experiment engines
             */
            this.labExperimentEngines = new LabExperimentEngine[this.farmSize];
            for (int unitId = 0; unitId < this.farmSize; unitId++) {
                this.labExperimentEngines[unitId] = new LabExperimentEngine(unitId, labManagement);
                if (this.labExperimentEngines[unitId] == null) {
                    throw new NullPointerException(LabExperimentEngine.class.getSimpleName());
                }
            }

            success = true;
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_Success_arg, success));

        return success;
    }

    /**
     *
     * @return
     */
    public boolean Start() {
        final String methodName = "Start";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        boolean success = false;

        try {
            /*
             * Create a new thread and start it
             */
            this.thread = new Thread(this);
            this.thread.start();

            /*
             * Give it a chance to start running and then check that it has started
             */
            for (int i = 0; i < 5; i++) {
                if ((success = this.running) == true) {
                    break;
                }

                Delay.MilliSeconds(500);
                System.out.println('?');
            }

            if (success == false) {
                throw new RuntimeException(STRERR_ThreadFailedToStart);
            }
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_Success_arg, success));

        return success;
    }

    /**
     *
     * @param experimentId
     * @param sbName
     * @return
     */
    public synchronized boolean Cancel(int experimentId, String sbName) {
        final String methodName = "Cancel";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_ExperimentIdSbName_arg2, experimentId, sbName));

        boolean success = false;

        try {
            /*
             * First, try cancelling the experiment on the queue
             */
            if ((success = this.labManagement.getExperimentQueue().Cancel(experimentId, sbName)) == false) {
                if (this.labExperimentEngines == null) {
                    throw new NullPointerException(STRERR_LabExperimentEngines);
                }

                /*
                 * Experiment may be currently running, find the engine it might be running on and cancel it there
                 */
                for (int unitId = 0; unitId < this.farmSize; unitId++) {
                    /*
                     * Get the lab experiment engine
                     */
                    LabExperimentEngine labExperimentEngine = this.labExperimentEngines[unitId];
                    if (labExperimentEngine == null) {
                        throw new NullPointerException(String.format(STRERR_LabExperimentEngineUnitId_arg, unitId));
                    }

                    if ((success = labExperimentEngine.Cancel(experimentId, sbName)) == true) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_Success_arg, success));

        return success;
    }

    /**
     *
     * @param userGroup
     * @param priorityHint
     * @return
     */
    public synchronized WaitEstimate GetEffectiveQueueLength(String userGroup, int priorityHint) {
        final String methodName = "GetEffectiveQueueLength";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_UserGroupPriorityHint_arg2, userGroup, priorityHint));

        /*
         * NOTE: This implementation does not consider the group or priority of the user
         */

        /*
         * Get the wait estimate of the queue
         */
        WaitEstimate waitEstimate = this.labManagement.getExperimentQueue().GetWaitEstimate();

        /*
         * Add in the time remaining before the next experiment can run
         */
        LabExperimentStatus labExperimentStatus = this.GetLabExperimentStatus(0, null);
        waitEstimate.setEstWait(waitEstimate.getEstWait() + labExperimentStatus.getExperimentStatus().getEstRemainingRuntime());

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName);

        return waitEstimate;
    }

    /**
     *
     * @param experimentId
     * @param sbName
     * @return
     */
    public synchronized LabExperimentStatus GetLabExperimentStatus(int experimentId, String sbName) {
        final String methodName = "GetLabExperimentStatus";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_ExperimentIdSbName_arg2, experimentId, sbName));

        LabExperimentStatus labExperimentStatus = new LabExperimentStatus();

        try {
            /*
             * Get the status of the experiment from the queue
             */
            StatusCodes statusCode = this.labManagement.getExperimentQueue().GetStatus(experimentId, sbName);
            switch (statusCode) {
                case Unknown:
                    /*
                     * The experiment never existed
                     */
                    labExperimentStatus.setExperimentStatus(new ExperimentStatus(StatusCodes.Unknown));
                    break;

                case Waiting:
                    /*
                     * Experiment is waiting on the queue
                     */
                    QueuedExperimentInfo queuedExperimentInfo = this.labManagement.getExperimentQueue().GetQueuedExperimentInfo(experimentId, sbName);
                    LabExperimentInfo labExperimentInfo = queuedExperimentInfo.getLabExperimentInfo();
                    if (labExperimentInfo != null) {
                        /*
                         * Get the queue position and wait time and add in the time for any currently running experiment
                         * ????
                         */
                        WaitEstimate waitEstimate = new WaitEstimate();
                        waitEstimate.setEffectiveQueueLength(queuedExperimentInfo.getPosition());
                        waitEstimate.setEstWait(queuedExperimentInfo.getWaitTime() + this.GetMinRemainingRuntime());

                        /*
                         * Set the experiment status and time it takes to run the experiment
                         */
                        ExperimentStatus experimentStatus = new ExperimentStatus(labExperimentInfo.getStatusCode());
                        experimentStatus.setEstRuntime(labExperimentInfo.getEstExecutionTime());
                        experimentStatus.setEstRemainingRuntime(labExperimentInfo.getEstExecutionTime());
                        experimentStatus.setWaitEstimate(waitEstimate);

                        labExperimentStatus = new LabExperimentStatus(experimentStatus);

                        Logfile.Write(logLevel, String.format(STRLOG_ExperimentStatus_arg4,
                                waitEstimate.getEffectiveQueueLength(), waitEstimate.getEstWait(),
                                experimentStatus.getEstRuntime(), experimentStatus.getEstRemainingRuntime()));
                    }
                    break;

                case Running:
                    /*
                     * Check each experiment engine in the farm looking for the specified experiment
                     */
                    for (int unitId = 0; unitId < this.farmSize; unitId++) {
                        /*
                         * Get the lab experiment engine
                         */
                        LabExperimentEngine labExperimentEngine = this.labExperimentEngines[unitId];
                        if (labExperimentEngine == null) {
                            throw new NullPointerException(String.format(STRERR_LabExperimentEngineUnitId_arg, unitId));
                        }

                        /*
                         * Check if the lab experiment engine is running this experiment
                         */
                        ExperimentStatus experimentStatus = labExperimentEngine.GetExperimentStatus(experimentId, sbName);
                        if (experimentStatus.getStatusCode() != StatusCodes.Unknown) {
                            /*
                             * This engine is running the experiment
                             */
                            labExperimentStatus.setExperimentStatus(experimentStatus);
                            break;
                        }
                    }

                    /*
                     * Check if an engine was found to be running this experiment
                     */
                    if (labExperimentStatus.getExperimentStatus() == null) {
                        labExperimentStatus.setExperimentStatus(new ExperimentStatus(StatusCodes.Unknown));
                    }
                    break;

                case Cancelled:
                    /*
                     * The experiment was cancelled while waiting on the queue
                     */
                    labExperimentStatus.setExperimentStatus(new ExperimentStatus(StatusCodes.Cancelled));
                    break;

                default:
                    /*
                     * Experiment has completed, cancelled or failed so get the status from the experiment results
                     */
                    ResultReport resultReport = this.labManagement.getExperimentResults().RetrieveResultReport(experimentId, sbName);
                    labExperimentStatus.setExperimentStatus(new ExperimentStatus(resultReport.getStatusCode()));
                    break;
            }
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
            labExperimentStatus.setExperimentStatus(new ExperimentStatus(StatusCodes.Unknown));
        }

        ExperimentStatus experimentStatus = labExperimentStatus.getExperimentStatus();

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_ExperimentStatus_arg3,
                experimentStatus.getStatusCode(), experimentStatus.getEstRuntime(), experimentStatus.getEstRemainingRuntime()));

        return labExperimentStatus;
    }

    /**
     *
     * @param userGroup
     * @return
     */
    public String GetXmlLabConfiguration(String userGroup) {
        final String methodName = "GetXmlLabConfiguration";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_UserGroup_arg, userGroup));

        String xmlLabConfiguration = null;

        try {
            /*
             * Load the lab configuration XML document from the file and convert to a string
             */
            Document document = XmlUtilities.GetDocumentFromFile(null, this.labManagement.getLabConfiguration().getFilename());
            xmlLabConfiguration = XmlUtilities.ToXmlString(document);
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName);

        return xmlLabConfiguration;
    }

    /**
     *
     * @return
     */
    public String GetLabInfo() {
        final String methodName = "GetLabInfo";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        String labInfo = this.labManagement.getLabConfiguration().getLabInfoUrl();

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName);

        return labInfo;
    }

    /**
     *
     * @return
     */
    public LabStatus GetLabStatus() {
        final String methodName = "GetLabStatus";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        LabStatus labStatus = new LabStatus(false, "");

        try {
            if (this.labExperimentEngines == null) {
                throw new NullPointerException(STRERR_LabExperimentEngines);
            }

            /*
             * Check lab status of each experiment engine
             */
            for (int unitId = 0; unitId < this.farmSize; unitId++) {
                LabExperimentEngine labExperimentEngine = this.labExperimentEngines[unitId];
                if (labExperimentEngine == null) {
                    throw new NullPointerException(String.format(STRERR_LabExperimentEngineUnitId_arg, unitId));
                }

                /*
                 * Keep a tally
                 */
                LabStatus engineLabStatus = labExperimentEngine.GetLabStatus();
                labStatus.setOnline(labStatus.isOnline() || engineLabStatus.isOnline());
                String message = String.format(STRLOG_UnitIdLabStatusMessage_arg2, unitId, engineLabStatus.getLabStatusMessage());
                labStatus.setLabStatusMessage(labStatus.getLabStatusMessage() + message);
            }
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
            labStatus.setLabStatusMessage(ex.toString());
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName);

        return labStatus;
    }

    /**
     *
     * @param experimentId
     * @param sbName
     * @return
     */
    public ResultReport RetrieveResult(int experimentId, String sbName) {
        final String methodName = "RetrieveResult";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_ExperimentIdSbName_arg2, experimentId, sbName));

        ResultReport resultReport;

        try {
            /*
             * Try getting the result of the completed experiment
             */
            resultReport = this.labManagement.getExperimentResults().RetrieveResultReport(experimentId, sbName);
            if (resultReport.getStatusCode() == StatusCodes.Unknown) {
                /*
                 * No results found for the experiment, check the queue to see if it ever existed
                 */
                StatusCodes statusCode = this.labManagement.getExperimentQueue().GetStatus(experimentId, sbName);
                resultReport.setStatusCode(statusCode);
            }
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
            resultReport = new ResultReport(StatusCodes.Unknown, ex.toString());
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_StatusCode_arg, resultReport.getStatusCode()));

        return resultReport;
    }

    /**
     *
     * @param experimentId
     * @param sbName
     * @param xmlSpecification
     * @param userGroup
     * @param priorityHint
     * @return
     */
    public synchronized SubmissionReport Submit(int experimentId, String sbName, String xmlSpecification, String userGroup, int priorityHint) {
        final String methodName = "Submit";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_ExperimentIdSbName_arg2, experimentId, sbName));

        SubmissionReport submissionReport = new SubmissionReport(experimentId);

        /*
         * Validate the experiment specification before submitting
         */
        ValidationReport validationReport = this.Validate(xmlSpecification, userGroup);
        if (validationReport.isAccepted() == true) {
            /*
             * Specification is valid, create an instance of the experiment
             */
            LabExperimentInfo labExperimentInfo = new LabExperimentInfo(experimentId, sbName);
            labExperimentInfo.setXmlSpecification(xmlSpecification);
            labExperimentInfo.setUserGroup(userGroup);
            labExperimentInfo.setPriorityHint(priorityHint);
            labExperimentInfo.setEstExecutionTime((int) validationReport.getEstRuntime());

            /*
             * Add the experiment to the queue
             */
            try {
                QueuedExperimentInfo queuedExperimentInfo = this.labManagement.getExperimentQueue().Enqueue(labExperimentInfo);
                if (queuedExperimentInfo == null) {
                    throw new RuntimeException(STRERR_FailedToEnqueueExperiment);
                }

                /*
                 * Update submission report current queue length and wait time
                 */
                WaitEstimate waitEstimate = new WaitEstimate();
                waitEstimate.setEffectiveQueueLength(queuedExperimentInfo.getQueueLength());
                waitEstimate.setEstWait(queuedExperimentInfo.getWaitTime() + this.GetMinRemainingRuntime());
                submissionReport.setWaitEstimate(waitEstimate);

                /*
                 * Update the statistics with revised wait estimate
                 */
                queuedExperimentInfo.setWaitTime((int) waitEstimate.getEstWait());
                this.labManagement.getExperimentStatistics().Submitted(queuedExperimentInfo);

                /*
                 * Tell lab experiment manager thread that an experiment has been submitted
                 */
                this.labManagement.getSignalSubmitted().Notify();
            } catch (Exception ex) {
                validationReport = new ValidationReport(ex.getMessage());
            }
        }

        submissionReport.setValidationReport(validationReport);

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_Accepted_arg, submissionReport.getValidationReport().isAccepted()));

        return submissionReport;
    }

    /**
     *
     * @param xmlSpecification
     * @param userGroup
     * @return
     */
    public ValidationReport Validate(String xmlSpecification, String userGroup) {
        final String methodName = "Validate";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        ValidationReport validationReport;

        try {
            if (this.labExperimentEngines == null) {
                throw new NullPointerException(STRERR_LabExperimentEngines);
            }

            /*
             * Pass to the first experiment engine to validate
             */
            LabExperimentEngine labExperimentEngine = this.labExperimentEngines[0];
            if (labExperimentEngine == null) {
                throw new NullPointerException(String.format(STRERR_LabExperimentEngineUnitId_arg, 0));
            }

            /*
             * Validate the specification
             */
            validationReport = labExperimentEngine.Validate(xmlSpecification);
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
            validationReport = new ValidationReport(ex.toString());
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_Accepted_arg, validationReport.isAccepted()));

        return validationReport;
    }

    /**
     *
     */
    public void Close() {
        final String methodName = "Close";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        try {
            if (this.labExperimentEngines == null) {
                throw new NullPointerException(STRERR_LabExperimentEngines);
            }

            /*
             * Check each experiment engine to see if it is running an experiment
             */
            for (int unitId = 0; unitId < this.farmSize; unitId++) {
                /*
                 * Get the experiment engine
                 */
                LabExperimentEngine labExperimentEngine = this.labExperimentEngines[unitId];
                if (labExperimentEngine == null) {
                    throw new NullPointerException(String.format(STRERR_LabExperimentEngineUnitId_arg, unitId));
                }

                /*
                 * Shutdown experiment engine
                 */
                labExperimentEngine.Close();
            }
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
        }

        /*
         * Shutdown experiment manager
         */
        if (this.running == true) {
            this.stopRunning = true;

            /*
             * Lab experiment manager thread may be waiting for an experiment submission signal
             */
            this.labManagement.getSignalSubmitted().Notify();

            /*
             * Wait for LabExperimentManager thread to terminate
             */
            try {
                this.thread.join();
            } catch (InterruptedException ex) {
                Logfile.WriteError(ex.toString());
            }
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName);
    }

    @Override
    public void run() {
        final String methodName = "run";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        /*
         * Initialise state machine
         */
        States lastState = States.Done;
        States thisState = States.Init;
        int nextUnit = 0;
        this.running = true;

        /*
         * Allow other threads to check the state of this thread
         */
        Delay.MilliSeconds(500);

        /*
         * State machine loop
         */
        try {
            boolean success;

            while (thisState != States.Done) {
                /*
                 * Display message on each state change
                 */
                if (thisState != lastState) {
                    String logMessage = String.format(STRLOG_StateChange_arg2, lastState.toString(), thisState.toString());
                    System.out.println(logMessage);
//                    Logfile.Write(logLevel, logMessage);

                    lastState = thisState;
                }

                switch (thisState) {
                    case Init:
                        /*
                         * Update lab status
                         */
                        this.labStatusMessage = StatusCodes.Ready.toString();

                        /*
                         * Revert any 'Running' experiments back to 'Waiting' so that they can be run again
                         */
                        ArrayList<LabExperimentInfo> labExperimentInfoList = this.labManagement.getExperimentQueue().RetrieveByStatus(StatusCodes.Running);
                        if (labExperimentInfoList != null) {
                            Iterator iterator = labExperimentInfoList.iterator();
                            while (iterator.hasNext()) {
                                LabExperimentInfo labExperimentInfo = (LabExperimentInfo) iterator.next();

                                /*
                                 * Change the experiment status to 'Waiting' so the experiment engine will find it to run again
                                 */
                                success = this.labManagement.getExperimentQueue().UpdateStatus(labExperimentInfo.getQueueId(), StatusCodes.Waiting);

                                /*
                                 * Delete the database statistics entry for this experiment
                                 */
                                ExperimentStatisticInfo experimentStatisticInfo = this.labManagement.getExperimentStatistics().RetrieveByExperimentId(
                                        labExperimentInfo.getExperimentId(), labExperimentInfo.getSbName());
                                if (experimentStatisticInfo != null) {
                                    this.labManagement.getExperimentStatistics().Delete(experimentStatisticInfo.getId());
                                }

                                /*
                                 * Delete the database results entry for this experiment
                                 */
                                ExperimentResultInfo experimentResultInfo = this.labManagement.getExperimentResults().RetrieveByExperimentId(
                                        labExperimentInfo.getExperimentId(), labExperimentInfo.getSbName());
                                if (experimentResultInfo != null) {
                                    this.labManagement.getExperimentResults().Delete(experimentResultInfo.getId());
                                }

                                String logMessage = String.format(STRLOG_RevertToWaiting_arg, labExperimentInfo.getExperimentId(), labExperimentInfo.getSbName(), success);
                                Logfile.Write(logMessage);
                            }
                        }

                        /*
                         * Check if any experiments have not notified their ServiceBroker
                         */
                        thisState = States.CheckNotified;
                        break;

                    case Idle:
                        /*
                         * Wait for an experiment to be submitted or timeout after a certain time. In either case, check
                         * the experiment queue. Maybe an experiment submission signal got missed and it didn't get seen
                         * here. It has happened before.
                         */
                        if (this.labManagement.getSignalSubmitted().Wait(INT_DelayCheckQueueSeconds * 1000) == true) {
                            /*
                             * Check if shutting down
                             */
                            if (this.stopRunning == true) {
                                thisState = States.StopRunning;
                                break;
                            }

                            /*
                             * An experiment has been submitted, go check the queue
                             */
                            this.labManagement.getSignalSubmitted().Reset();
                            thisState = States.CheckQueue;
                            break;
                        }

                        /*
                         * Timed out, go check some other things
                         */
                        thisState = States.Maintenance;
                        break;

                    case CheckQueue:
                        /*
                         * Check the queue to see if there are any experiments waiting
                         */
                        if (this.labManagement.getExperimentQueue().GetCountWaiting() > 0) {
                            thisState = States.StartEngine;
                            break;
                        }

                        thisState = States.Idle;
                        break;

                    case StartEngine:
                        /*
                         * Find an available experiment engine to run a waiting experiment
                         */
                        boolean foundAvailable = false;
                        for (int i = 0; i < this.farmSize; i++) {
                            /*
                             * Get the experiment engine
                             */
                            LabExperimentEngine labExperimentEngine = this.labExperimentEngines[nextUnit];

                            String logMessage = String.format("%d: UnitId: %d  Running: %s",
                                    i, labExperimentEngine.getUnitId(), labExperimentEngine.isRunning());
                            System.out.println(logMessage);

                            /*
                             * Determine if this experiment engine is currently running
                             */
                            if (labExperimentEngine.isRunning() == false) {
                                /*
                                 * Not running, try starting it
                                 */
                                if (labExperimentEngine.Start() == true) {
                                    /*
                                     * The available engine has been started, wait a bit before checking the queue
                                     */
                                    foundAvailable = true;
                                    Delay.MilliSeconds(1000);
                                    break;
                                }
                            }

                            /*
                             * Determine which engine to try starting next
                             */
                            if (++nextUnit == this.farmSize) {
                                nextUnit = 0;
                            }
                        }

                        /*
                         * Check if there are any engines available
                         */
                        if (foundAvailable == false) {
                            /*
                             * No engines available to start running
                             */
                            thisState = States.Idle;
                            break;
                        }

                        thisState = States.CheckNotified;
                        break;

                    case CheckNotified:
                        /*
                         * Check if any experiments have not notified their ServiceBroker
                         */
                        success = true;
                        ArrayList<ExperimentResultInfo> allNotNotified = this.labManagement.getExperimentResults().RetrieveAllNotNotified();
                        if (allNotNotified != null) {
                            Iterator iterator = allNotNotified.iterator();
                            while (iterator.hasNext() && success == true) {
                                ExperimentResultInfo experimentResultInfo = (ExperimentResultInfo) iterator.next();
                                success = this.labExperimentEngines[0].NotifyServiceBroker(
                                        experimentResultInfo.getExperimentId(), experimentResultInfo.getSbName());
                            }
                        }
                        
                        thisState = States.CheckQueue;
                        break;

                    case Maintenance:
                        thisState = States.Idle;
                        break;

                    case StopRunning:
                        thisState = States.Done;
                        break;
                }
            }
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
        }

        /*
         * Thread is no longer running
         */
        this.running = false;

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName);
    }

    /**
     *
     * @return
     */
    private int GetMinRemainingRuntime() {
        final String methodName = "Validate";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        int minRemainingRuntime = Integer.MAX_VALUE;

        try {
            if (this.labExperimentEngines == null) {
                throw new NullPointerException(STRERR_LabExperimentEngines);
            }

            /*
             * Get the remaining runtime for each experiment engine
             */
            for (int unitId = 0; unitId < this.farmSize; unitId++) {
                /*
                 * Get the experiment engine
                 */
                LabExperimentEngine labExperimentEngine = this.labExperimentEngines[unitId];
                if (labExperimentEngine == null) {
                    throw new NullPointerException(String.format(STRERR_LabExperimentEngineUnitId_arg, unitId));
                }

                /*
                 * Get the remaining runtime for this engine and check if this is a smaller value
                 */
                int remainingRuntime = labExperimentEngine.GetRemainingRuntime();
                if (remainingRuntime < minRemainingRuntime) {
                    minRemainingRuntime = remainingRuntime;
                }
            }
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName);

        return minRemainingRuntime;
    }
}