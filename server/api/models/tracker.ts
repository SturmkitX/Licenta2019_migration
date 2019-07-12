import * as mongoose from 'mongoose';
import {AccessPointSchema} from "./ap-pref";

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
    lastUpdated: {
        type: Number,
        default: 0
    },
    lastPosition: {
        type: Schema.Types.ObjectId,
        ref: 'History',
        default: null
    },
    history: [{
        type: Schema.Types.ObjectId,
        ref: 'History',
        default: []
    }],
    aps: [{
        type: AccessPointSchema,
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