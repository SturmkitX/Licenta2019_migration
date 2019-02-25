import {UserTracker} from "./tracker";

export class User {
    _id: string;
    __v: number;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    role: string;
    trackers: UserTracker[];
}