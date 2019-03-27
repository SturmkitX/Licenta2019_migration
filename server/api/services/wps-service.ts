import {DecodedPosition} from "../models/history-interm";
import * as XMLHttp from 'xmlhttprequest';

export class WpsService {
    constructor() {}

    private serverAddr: string = 'http://api.mylnikov.org/geolocation/wifi?v=1.1&data=open&search=';

    public getPosition(stations: any): DecodedPosition {
        let rawData = '';
        for (let station of stations) {
            rawData += (station.mac + ',' + station.rssi + ';');
        }
        rawData = rawData.slice(0, rawData.length - 1);
        console.log('WPS raw data: ' + rawData);
        const encodedData = Buffer.from(rawData).toString('base64');

        let request = new XMLHttp.XMLHttpRequest();
        request.open("GET", `${this.serverAddr}${encodedData}`, false);
        request.send();
        const position: DecodedPosition = JSON.parse(request.responseText);
        return position;
    }
}