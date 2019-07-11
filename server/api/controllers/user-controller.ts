import { User } from '../models/user';
import { Request, Response } from 'express';
import * as bcrypt from 'bcryptjs';

export class UserController{

    constructor() {}

    /* ADMIN specific methods */

    public getAll(req: Request, res: Response): void {
        // @ts-ignore
        console.log(req.user);
        User.find()
            .exec((err: any, users: Document[]) => {
                if (err) {
                    res.status(500).send(err);
                } else {
                    res.status(200).json(users);
                }
            });
    }

    public getSpecificUser(req: Request, res: Response): void {
        User.findById(req.params.userId, (err: any, userDoc: Document) => {
            const user: any = userDoc;
            if (err) {
                res.status(500).send(err);
            } else if (!user) {
                res.status(404).json(null);
            } else {
                user.password = null;
                res.status(200).json(user);
            }
        });
    }

    public saveUser(req: Request, res: Response): void {
        if (!req.body) {
            res.status(400).json(null);
        }

        const user = new User({
            firstName: req.body.firstName,
            lastName: req.body.lastName,
            email: req.body.email,
            password: bcrypt.hashSync(req.body.password, 10),
            trackers: req.body.trackers     // || []
        }).save((err, userSave) => {
            if (err) {
                res.status(500).send(err);
            } else {
                res.status(200).json(userSave);
            }
        });
    }


    /* USER + ADMIN methods */
    public getSelf(req: Request, res: Response): void {
        // @ts-ignore
        User.findById(req.user.id, {trackers: 0, password: 0})
            .exec((err: any, userDoc: Document) => {
                const user: any = userDoc;
                if (err) {
                    res.status(500).send(err);
                } else if (!user) {
                    res.status(404).json(null);
                } else {
                    // user.password = null;
                    res.status(200).json(user);
                }
            });
    }

    public updateSelf(req: Request, res: Response) {
        // @ts-ignore
        if (!req.user || !req.user.id) {
            res.status(403).send('You are not logged in');
            return;
        }

        if (req.body.password == null) {
            delete req.body.password;
        }

        // @ts-ignore
        User.updateOne({_id: req.user.id}, req.body, (err, user) => {
            if (err) {
                res.status(404).send(err);
            } else {
                // user successfully updated
                res.status(200).json(user);
            }
        });
    }
}
