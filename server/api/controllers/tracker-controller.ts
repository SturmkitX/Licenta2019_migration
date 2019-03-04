import { Tracker } from '../models/tracker';
import { Request, Response } from 'express';
import {User} from "../models/user";

export class TrackerController{

    constructor() {}

    /* ADMIN specific methods */

    public getAll(req: Request, res: Response): void {
        Tracker.find()
            .populate('history')
            .exec((err: any, trackers: Document[]) => {
                if (err) {
                    res.status(500).send(err);
                } else {
                    res.status(200).json(trackers);
                }
            });
    }

    public getSpecific(req: Request, res: Response): void {
        Tracker.findById(req.params.trackerId, (err: any, tracker: Document) => {
            if (err) {
                res.status(500).send(err);
            } else if (!tracker) {
                res.status(404).json(null);
            } else {
                res.status(200).json(tracker);
            }
        });
    }

    public saveTracker(req: Request, res: Response): void {
        if (!req.body) {
            res.status(400).json(null);
        }

        new Tracker(req.body).save((err, trackerSave) => {
            const tracker: any = trackerSave;
            if (err) {
                res.status(500).send(err);
            } else {
                User.findByIdAndUpdate(tracker.userId, {$push: {trackers: tracker._id}},
                    (err, user) => {
                        if (err) {
                            res.status(500).send(err);
                        }
                    });

                res.status(200).json(trackerSave);
            }
        });
    }


    /* USER + ADMIN methods */
    public getSelf(req: Request, res: Response): void {
        // @ts-ignore
        Tracker.find({userId: req.user.id})
            .populate('history')
            .exec((err: any, trackers: Document[]) => {
                if (err) {
                    res.status(500).send(err);
                } else {
                    res.status(200).json(trackers);
                }
            });
    }

    public updateOwnTracker(req: Request, res: Response): void {
        const tracker = req.body;
        Tracker.updateOne({_id: tracker._id}, tracker, (err, trackerDoc) => {
            if (err) {
                res.status(500).send(err);
            } else if (!trackerDoc) {
                res.status(400).send(null);
            } else {
                res.status(200).json(trackerDoc);
            }
        });
    }
}
