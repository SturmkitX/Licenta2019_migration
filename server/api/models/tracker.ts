import * as mongoose from 'mongoose';

const Schema = mongoose.Schema;

const TrackerSchema = new Schema({
    rfId: [{
        type: Number,
        required: 'You need to associate an RFID'
    }],
    name: {
        type: String,
        default: ''
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
    preferredMethod: {
        type: String,
        default: 'None'
    },
    history: [{
        type: Schema.Types.ObjectId,
        ref: 'History',
        default: []
    }],
    userId: {
        type: Schema.Types.ObjectId,
        ref: 'User',
        required: 'You need to specify who this tracker belongs to'
    }
});

const Tracker = mongoose.model('Tracker', TrackerSchema);
export { Tracker };