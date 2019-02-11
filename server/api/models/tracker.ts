import * as mongoose from 'mongoose';

const Schema = mongoose.Schema;

const TrackerSchema = new Schema({
    rfId: {
        type: String,
        required: 'You need to associate an RFID'
    },
    lost: {
        type: Boolean,
        default: false
    },
    alarmActive: {
        type: Boolean,
        default: false
    },
    gpsActive: {
        type: Boolean,
        default: false
    },
    wifiActive: {
        type: Boolean,
        default: false
    },
    history: {
        type: [mongoose.Types.ObjectId],
        ref: 'History',
        default: []
    }
});

const Tracker = mongoose.model('Tracker', TrackerSchema);
export { Tracker };