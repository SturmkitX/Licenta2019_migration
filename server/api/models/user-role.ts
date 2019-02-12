import * as mongoose from 'mongoose';

const Schema = mongoose.Schema;

const RoleSchema = new Schema({
    role: {
        type: String,
        required: 'Role name must be specified'
    }
});

const UserRole = mongoose.model('UserRole', RoleSchema);
export { UserRole };