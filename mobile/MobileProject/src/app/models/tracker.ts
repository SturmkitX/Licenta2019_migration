import {UserHistory} from './history';

export class UserTracker {
    _id: string;
    __v: number;
    rfId: string;
    lost: boolean;
    alarmActive: boolean;
    gpsActive: boolean;
    wifiActive: boolean;
    history: UserHistory[];
    userId: string;
}