import { History } from '../models/history';
import { Tracker } from '../models/tracker';
import { Request, Response } from 'express';
import {DecodedPosition, HistoryInterm} from "../models/history-interm";
import {WpsService} from "../services/wps-service";

export class HistoryController{

    private wpsService: WpsService;

    private readonly WPS_WEIGHT = 0.7;

    constructor() {
        this.wpsService = new WpsService();
    }

    /* ADMIN specific methods */

    public getAll(req: Request, res: Response): void {
        History.find()
            .exec((err: any, entries: Document[]) => {
                if (err) {
                    res.status(500).send(err);
                } else {
                    res.status(200).json(entries);
                }
            });
    }

    public getSpecific(req: Request, res: Response): void {
        History.findById(req.params.historyId, (err: any, entry: Document) => {
            if (err) {
                res.status(500).send(err);
            } else if (!entry) {
                res.status(404).json(null);
            } else {
                res.status(200).json(entry);
            }
        });
    }

    public saveHistory(req: Request, res: Response): void {
        if (!req.body) {
            res.status(400).json(null);
            return;
        }

        console.log('History received from Arduino!');
        console.log(req.body);

        // get the associated tracker
        Tracker.findOne({rfId: req.body.rfId}, (err, tracker) => {
            if (tracker == null) {
                res.status(404).send(err);
                return;
            } else {
                const trackerId = tracker._id;

                // @ts-ignore
                const method = tracker.preferredMethod;

                if (method === 'None') {
                    res.status(200).send();
                    return;
                }
                const interm : HistoryInterm = this.prepareHistory(req.body.positions, trackerId, method);
                console.log('Interm:');
                console.log(interm);
                this.saveHistoryInterm(interm, req, res);
            }
        });
    }


    /* USER + ADMIN methods */
    public getSelfForTracker(req: Request, res: Response): void {
        // the user specifies the id of a tracker
        // 1. check if the user owns that tracker
        // @ts-ignore
        const userId = req.user.id;
        Tracker.findOne({_id: req.params.trackerId, userId: userId})
            .populate('history')
            .exec((err: any, tracker: Document) => {
                if (err) {
                    res.status(500).send(err);
                } else if (!tracker) {
                    res.status(404).json([]);
                } else {
                    // @ts-ignore
                    res.status(200).json(tracker.history);
                }
            });
    }

    private prepareHistory(positions, trackerId, method : string): HistoryInterm {
        let wifiPosition: HistoryInterm = null;
        let gpsPosition: HistoryInterm = null;
        let interpResult: HistoryInterm = null;
        for (let position of positions) {
            if (position.source === 'WIFI' && method.includes('WPS')) {
                // get the list of macs and their rssi
                let decodedData: DecodedPosition = this.wpsService.getPosition(position.macs);
                console.log('WPS data result: ' + decodedData.result);
                if (decodedData.result == 200) {
                    wifiPosition = {
                        trackerId: trackerId,
                        lat: decodedData.data.lat,
                        lng: decodedData.data.lon,
                        range: decodedData.data.range,
                        source: 'WIFI'
                    };
                }
            } else if (position.source === 'GPS' && method.includes('GPS')) {
                // gps data is already present
                gpsPosition = {
                    trackerId: trackerId,
                    lat: position.latitude,
                    lng: position.longitude,
                    range: 50,   // mock
                    source: 'GPS'
                };
            }
        }

        // check exclusions
        if (wifiPosition == null) {
            return gpsPosition;
        } else if (gpsPosition == null) {
            return wifiPosition;
        } else {
            interpResult = {
                trackerId: trackerId,
                lat: wifiPosition.lat * this.WPS_WEIGHT + gpsPosition.lat * (1.0 - this.WPS_WEIGHT),
                lng: wifiPosition.lng * this.WPS_WEIGHT + gpsPosition.lng * (1.0 - this.WPS_WEIGHT),
                range: wifiPosition.range * this.WPS_WEIGHT + gpsPosition.range * (1.0 - this.WPS_WEIGHT),
                source: 'WPS + GPS'
            };
        }

        return interpResult;
    }

    private saveHistoryInterm(interm: HistoryInterm, req: Request, res: Response) {
        new History(interm).save((err, entry) => {
            if (err) {
                res.status(500).send(err);
            } else {
                // update the parent tracker

                // @ts-ignore
                Tracker.findByIdAndUpdate(entry.trackerId, {$push: {history: entry._id}, lastUpdated: entry.creationDate,
                    lastPosition: entry._id},
                    (err, tracker) => {
                        if (err) {
                            res.status(500).send(err);
                            return;
                        }
                    });
                res.status(200).json(entry);
            }
        });
    }
}
