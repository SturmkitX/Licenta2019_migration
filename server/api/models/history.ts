import * as mongoose from 'mongoose';

const Schema = mongoose.Schema;

const HistorySchema = new Schema({
    trackerId: {
        type: mongoose.Types.ObjectId,
        ref: 'Tracker',
        required: 'Must specify tracker ID'
    },
    lat: {
        type: Number,
        required: 'Must specify latitude'
    },
    lng: {
        type: Number,
        required: 'Must specify longitude'
    },
    creationDate: {
        type: Date,
        default: Date.now
    },
    source: {
        type: String
    }
});

const History = mongoose.model('History', HistorySchema);
export { History };