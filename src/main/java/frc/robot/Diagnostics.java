package frc.robot;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.List;
import java.util.ArrayList;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.FaultID;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;

/**
 * Diagnostics defines some tabs in the shuffleboard to display diagnostics for the motors 
 * and power distribution panel.
 */
public class Diagnostics {
 
    /* DataType defines the motor attributes to monitor.  The values here are a sample set. 
       Update this enumeration to define attributes you care about. */
    enum DataType {FAULTS, STICKY_FAULTS, TEMP, INVERTED_STATE, POSITION, VELOCITY};

    /* PowerDataType defines the power distribution panel  to monitor.  The values here are a sample set, 
       update this enumeratio to define the attributes you care about. */
    enum PowerDataType {VOLTAGE, TEMP, CURRENT, ENERGY };

    /* Define tabs in the shuffleboard */
    private final ShuffleboardTab summaryTab = Shuffleboard.getTab("Summary");
    private final ShuffleboardTab motorTab = Shuffleboard.getTab("Motors");
    private final ShuffleboardTab powerTab = Shuffleboard.getTab("Power");

    /* Define an entry to display an overall fault status.  This controls a "Fault Indicator" widget
       in the Summary tab */
    private NetworkTableEntry faultEntry;
    
    /* stores an array of the motors */
    private CCSparkMax[] motors;

    /* the power distribution panel */
    private PowerDistribution pdp = new PowerDistribution(0, PowerDistribution.ModuleType.kCTRE);

    /* The motorEntryMap allows us to map a motor name to a NetworkTableEntry.  See the getEntry() method on how
       to use motorEntryMap.  */
    // key -> motor name, value -> map (key -> DataType, value -> NetworkTableEntry)
    private Map<String, Map<DataType, NetworkTableEntry>> motorEntryMap = new HashMap<>();

    // key -> PowerDataType, value -> NetworkTableEntry
    private Map<PowerDataType, NetworkTableEntry> powerEntryMap = new HashMap<>();
    private List<NetworkTableEntry> powerChannels = new ArrayList<>();
    private final static int NUM_POWER_CHANNELS = 8;

    /* contructor, saves the injected motors */
    public Diagnostics(CCSparkMax... motors) {
        this.motors = motors;
    }

    /* Creates the diagnostic widgets and associated NetworkTableEntries in the appropriate tabs.  In this examples,
       it displays a "Fault Indicator" widget in the Summary tab to indicate overall health of all motors. 
       It also displays a row of diagnostic widgets for each motor, so in the Motors tab there will be 4 rows.
       For the Power Distribution Panel, it create a set of widgets in the "Power" tab corresponding to the
       PowerDataType enumerations.  For Current, it displays current for the first 8 channels. */
    public void init() {
        
        faultEntry = summaryTab
          .add("Fault Indicator", false)
          .withWidget(BuiltInWidgets.kBooleanBox)
          .getEntry();

        int row = 0;
        final int faultsWidth = 2;
        // for each motor: Faults, Sticky Faults, Temp, Inverted state, position, velocity
        for(CCSparkMax m : motors) {
            int col = 0;
            Map<DataType, NetworkTableEntry> entryMap = new EnumMap<>(DataType.class);

            // initialize motorEntryMap
            motorEntryMap.put(m.getName(), entryMap);

            final String name = m.getName();
            final String shortName = m.getShortName();

            // FAULTS
            entryMap.put(DataType.FAULTS, motorTab.add(name + " faults", "")
            .withWidget(BuiltInWidgets.kTextView)
            .withPosition(col, row) 
            .withSize(faultsWidth, 1)
            .getEntry() );
            col += faultsWidth;
            
            // STICKY_FAULTS
            entryMap.put(DataType.STICKY_FAULTS, motorTab.add(shortName + " sticky faults", "")
            .withWidget(BuiltInWidgets.kTextView)
            .withPosition(col, row) 
            .withSize(faultsWidth, 1)
            .getEntry() );
            col += faultsWidth;
            
            // INVERTED_STATE
            entryMap.put(DataType.INVERTED_STATE, motorTab.add(shortName + " inv. state", "")
            .withWidget(BuiltInWidgets.kTextView)
            .withPosition(col++, row) 
            .withSize(1, 1)
            .getEntry() );

            // POSITION
            entryMap.put(DataType.POSITION, motorTab.add(shortName + "  position", 0)
            .withWidget(BuiltInWidgets.kTextView)
            .withPosition(col++, row) 
            .withSize(1, 1)
            .getEntry() );

            // TEMP
            entryMap.put(DataType.TEMP, motorTab.add(shortName + " temp", 0)
            .withWidget(BuiltInWidgets.kDial)
            .withPosition(col++, row) 
            .withSize(1, 1)
            .withProperties(Map.of("Min", 10, "Max", 100))  //celsius
            .getEntry() );
            
            // VELOCITY
            entryMap.put(DataType.VELOCITY, motorTab.add(shortName + "  velocity", 0)
            .withWidget(BuiltInWidgets.kDial)
            .withPosition(col++, row) 
            .withSize(1, 1)
            .getEntry() );
            
            row++;
        }

        row = 0;
        int col = 0;
        // Power tab

        // Voltage
        powerEntryMap.put(PowerDataType.VOLTAGE, powerTab.add("Voltage", 0)
            .withWidget(BuiltInWidgets.kDial)
            .withPosition(col++, row)
            .withSize(1,1)
            .getEntry());

        // temperature
        powerEntryMap.put(PowerDataType.TEMP, powerTab.add("Temperature", 0)
            .withWidget(BuiltInWidgets.kDial)
            .withPosition(col++, row)
            .withSize(1,1)
            .getEntry());

        // total current
        powerEntryMap.put(PowerDataType.CURRENT, powerTab.add("Total Current", 0)
            .withWidget(BuiltInWidgets.kDial)
            .withPosition(col++, row)
            .withSize(1,1)
            .getEntry());

        powerEntryMap.put(PowerDataType.ENERGY, powerTab.add("Total Energy", 0)
            .withWidget(BuiltInWidgets.kDial)
            .withPosition(col++, row)
            .withSize(1,1)
            .getEntry());
        
        row++;
        col = 0;
        for (int i=0; i < NUM_POWER_CHANNELS; i++) {
            powerChannels.add(powerTab.add("Channel " + i + " current", 0)
                .withWidget(BuiltInWidgets.kDial)
                .withPosition(col++, row)
                .withSize(1,1)
                .getEntry());
        }
        Shuffleboard.selectTab("Motors");
    }
    
    private void updateFaultStatus(NetworkTableEntry entry, CCSparkMax motor) {
        int fault = motor.getFaults();
        String faultMsg = "No fault";
        if (fault != 0) {
            StringJoiner sj = new StringJoiner(",");
            for(CANSparkMax.FaultID faultId : FaultID.values()) {
                if (motor.getFault(faultId)) {
                    sj.add(faultId.name());
                }
            }
            faultMsg = sj.toString();
        }
        entry.setString(faultMsg);
    }

    private void updateStickyFaultStatus(NetworkTableEntry entry, CCSparkMax motor) {
        int fault = motor.getStickyFaults();
        String faultMsg = "No fault";
        if (fault != 0) {
            StringJoiner sj = new StringJoiner(",");
            for(CANSparkMax.FaultID faultId : FaultID.values()) {
                if (motor.getStickyFault(faultId)) {
                    sj.add(faultId.name());
                }
            }
            faultMsg = sj.toString();
        }
        entry.setString(faultMsg);
    }

    /**
     * getEntry returns a NetworkTableEntry for a given motor and DataType.
     * 
     * @param motor the specific motor
     * @param type motor attribute type
     * @return the NetworkTableEntry for the given inputs
     */
    private NetworkTableEntry getEntry(CCSparkMax motor, DataType type) {
        return motorEntryMap.get(motor.getName()).get(type);
        
    }

    private void updateFaultStatus(CCSparkMax motor, DataType type) {
        if (type.equals(DataType.FAULTS)) {
            updateFaultStatus(getEntry(motor,type), motor);
        } else {
            updateStickyFaultStatus(getEntry(motor,type), motor);
        }
    }

    private void updateDoubleStatus(CCSparkMax motor, DataType type) {
        
        switch(type) {
            case TEMP:
                getEntry(motor,type).setDouble(motor.getMotorTemperature());
                break;
            case POSITION:
                getEntry(motor,type).setString(Double.toString(motor.getEncoder().getPosition()));
                break;
            case VELOCITY:
                getEntry(motor,type).setDouble(motor.getEncoder().getVelocity());
                break;
            default:
                break;
        }
    }

    private void updateStatus(CCSparkMax motor, DataType type) {

        switch(type) {
            case FAULTS:
            case STICKY_FAULTS:
                updateFaultStatus(motor, type);
                break;
            case TEMP:
            case POSITION:
            case VELOCITY:
                updateDoubleStatus(motor,type);
                break;
            case INVERTED_STATE: {
                String msg = motor.getInverted() ? "inverted" : "";
                getEntry(motor, type).setString(msg);
            }
            break;
        }
    }

    private void updatePowerStatus(PowerDataType dataType) {

        double value = 0.0 ;
        switch(dataType) {
            case VOLTAGE:
                value = pdp.getVoltage();
                break;
            case TEMP:
                value = pdp.getTemperature();
                break;
            case CURRENT:
                value = pdp.getTotalCurrent();
                break;
            case ENERGY:
                value = pdp.getTotalEnergy();
                break;
            default:
                System.err.println("Unsupported PowerDataType : " + dataType);
                return;
        }
        powerEntryMap.get(dataType).setDouble(value);
    }

    private void updateCurrentStatus(int channel) {
        if (channel < powerChannels.size()) {
            powerChannels.get(channel).setDouble(pdp.getCurrent(channel));
        } else {
            System.err.println("Invalid channel: " + channel);
        }
    }

    public void updateStatus() {
       
        int allFaults = 0;
        for (CCSparkMax motor : motors) {
            allFaults += motor.getFaults();
        }

        // boolean status
        faultEntry.setBoolean(allFaults == 0);

        // update status on SparkMax controllers
        for (CCSparkMax motor : motors) {
            for(DataType type : DataType.values()) {
                updateStatus(motor, type);
            }
        }

        // update status of Power Distribution Panel
        for (PowerDataType type : PowerDataType.values()) {
            updatePowerStatus(type);
        }

        // update current for individual channels
        for(int i=0, size = powerChannels.size(); i < size; i++) {
            updateCurrentStatus(i);
        }
        
    }
}
