/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uq.ilabs.library.labequipment;

import java.util.logging.Level;
import uq.ilabs.library.lab.utilities.Logfile;
import uq.ilabs.library.labequipment.devices.DeviceEquipment;
import uq.ilabs.library.labequipment.drivers.DriverEquipment;
import uq.ilabs.library.labequipment.engine.LabEquipmentConfiguration;
import uq.ilabs.library.labequipment.engine.LabEquipmentEngine;
import uq.ilabs.library.labequipment.engine.drivers.DriverGeneric;

/**
 *
 * @author uqlpayne
 */
public class EquipmentEngine extends LabEquipmentEngine {

    //<editor-fold defaultstate="collapsed" desc="Constants">
    private static final String STR_ClassName = EquipmentEngine.class.getName();
    private static final Level logLevel = Level.INFO;
    //</editor-fold>

    /**
     *
     * @param labEquipmentConfiguration
     * @throws Exception
     */
    public EquipmentEngine(LabEquipmentConfiguration labEquipmentConfiguration) throws Exception {
        super(labEquipmentConfiguration);

        final String methodName = "EquipmentEngine";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        /*
         * Nothing to do here
         */

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName);
    }

    /**
     *
     * @param setupId
     * @return
     * @throws Exception
     */
    @Override
    protected DriverGeneric GetDriver(String setupId) throws Exception {
        final String methodName = "GetDriver";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_SetupId_arg, setupId));

        DriverGeneric driverGeneric;

        /*
         * Create an instance of the driver for the specified setup Id
         */
        switch (setupId) {
            case Consts.STRXML_SetupId_Equipment:
                driverGeneric = new DriverEquipment(this.labEquipmentConfiguration);
                break;
            default:
                driverGeneric = super.GetDriver(setupId);
                break;
        }

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName,
                driverGeneric.getDriverName());

        return driverGeneric;
    }

    /**
     *
     * @return
     */
    @Override
    protected boolean PowerupEquipment() {
        final String methodName = "PowerupEquipment";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        boolean success = true;

        /*
         * YOUR CODE HERE
         */

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_Success_arg, success));

        return success;
    }

    /**
     *
     * @return
     */
    @Override
    protected boolean InitialiseEquipment() {
        final String methodName = "InitialiseEquipment";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        boolean success = false;

        try {
            /*
             * Create and initialise the equipment device
             */
            DeviceEquipment deviceEquipment = new DeviceEquipment(this.labEquipmentConfiguration);
            success = deviceEquipment.Initialise();
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
    @Override
    protected boolean PowerdownEquipment() {
        final String methodName = "PowerdownEquipment";
        Logfile.WriteCalled(logLevel, STR_ClassName, methodName);

        boolean success = true;

        /*
         * YOUR CODE HERE
         */

        Logfile.WriteCompleted(logLevel, STR_ClassName, methodName,
                String.format(STRLOG_Success_arg, success));

        return success;
    }
}
