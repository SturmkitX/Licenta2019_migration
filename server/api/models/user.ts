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
    role: {
        type: Schema.Types.ObjectId,
        ref: 'UserRole',
        required: 'Role must be specified',
        default: new mongoose.Types.ObjectId('5c62e003d6ca8b1299d0799c')    // _id of USER role
    },
    trackers: {
        type: [Schema.Types.ObjectId],
        ref: 'Tracker',
        default: []
    }
});

const User = mongoose.model('User', UserSchema);
export { User };