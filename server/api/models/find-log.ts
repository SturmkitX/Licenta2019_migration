import * as mongoose from 'mongoose';

const Schema = mongoose.Schema;

const FindLogSchema = new Schema({
    userId: {
        type: Schema.Types.ObjectId,
        ref: 'User',
        required: 'Please input the id of the user'
    },
    trackerId: {
        type: Schema.Types.ObjectId,
        ref: 'Tracker',
        required: 'Please input the id of the tracker'
    },
    date: {
        type: Date,
        default: Date.now
    }
});

const FindLog = mongoose.model('FindLog', FindLogSchema);
export { FindLog };