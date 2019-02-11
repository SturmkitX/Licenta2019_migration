import * as mongoose from 'mongoose';

const Schema = mongoose.Schema;

const UserSchema = new Schema({
    firstName: {
        type: String,
        required: 'Enter a first name'
    },
    lastName: {
        type: String,
        required: 'Enter a last name'
    },
    email: {
        type: String,
        required: 'Enter email',
        unique: true,
    },
    password: {
        type: String,
        required: 'Please enter password'
    },
    trackers: {
        type: [Schema.Types.ObjectId],
        ref: 'Tracker',
        default: []
    }
});

const User = mongoose.model('User', UserSchema);
export { User };