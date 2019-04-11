import * as mongoose from 'mongoose';

const Schema = mongoose.Schema;

const EmailSchema = new Schema({
    user: {
        type: Schema.Types.ObjectId,
        ref: 'User',
        required: 'Please input the id of the user'
    },
    message: {
        type: String,
        required: 'Please input the message'
    },
    date: {
        type: Date,
        default: Date.now
    },
    sent: {
        type: Boolean,
        default: false
    }
});

const Email = mongoose.model('Email', EmailSchema);
export { Email };