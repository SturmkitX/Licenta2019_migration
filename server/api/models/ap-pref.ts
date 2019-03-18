import * as mongoose from 'mongoose';

const Schema = mongoose.Schema;

const AccessPointSchema = new Schema({
    ssid: {
        type: String
    },
    password: {
        type: String
    },
    active: {
        type: Boolean
    }
});

const AccessPoint = mongoose.model('AccessPoint', AccessPointSchema);
export { AccessPoint, AccessPointSchema };